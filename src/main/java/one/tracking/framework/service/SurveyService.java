/**
 *
 */
package one.tracking.framework.service;

import static one.tracking.framework.entity.DataConstants.TOKEN_CONFIRM_LENGTH;
import static one.tracking.framework.entity.DataConstants.TOKEN_SURVEY_LENGTH;
import static one.tracking.framework.entity.DataConstants.TOKEN_VERIFY_LENGTH;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import one.tracking.framework.dto.RegistrationDto;
import one.tracking.framework.dto.SurveyStatusDto;
import one.tracking.framework.dto.SurveyStatusType;
import one.tracking.framework.dto.VerificationDto;
import one.tracking.framework.entity.SurveyInstance;
import one.tracking.framework.entity.SurveyResponse;
import one.tracking.framework.entity.SurveyStatus;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.Verification;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.ReleaseStatusType;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.question.BooleanQuestion;
import one.tracking.framework.entity.meta.question.ChecklistEntry;
import one.tracking.framework.entity.meta.question.ChecklistQuestion;
import one.tracking.framework.entity.meta.question.ChoiceQuestion;
import one.tracking.framework.entity.meta.question.Question;
import one.tracking.framework.repo.SurveyInstanceRepository;
import one.tracking.framework.repo.SurveyRepository;
import one.tracking.framework.repo.SurveyResponseRepository;
import one.tracking.framework.repo.SurveyStatusRepository;
import one.tracking.framework.repo.UserRepository;
import one.tracking.framework.repo.VerificationRepository;
import one.tracking.framework.util.JWTHelper;

/**
 * @author Marko Voß
 *
 */
@Service
public class SurveyService {

  public static final Instant INSTANT_MIN = Instant.ofEpochMilli(Long.MIN_VALUE);
  public static final Instant INSTANT_MAX = Instant.ofEpochMilli(Long.MAX_VALUE);

  private static final Logger LOG = LoggerFactory.getLogger(SurveyService.class);

  private static final Random RANDOM = new Random();

  @Autowired
  private SurveyRepository surveyRepository;

  @Autowired
  private SurveyInstanceRepository surveyInstanceRepository;

  @Autowired
  private VerificationRepository verificationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private SurveyResponseRepository surveyResponseRepository;

  @Autowired
  private SurveyStatusRepository surveyStatusRepository;

  @Autowired
  private SendGridService emailService;

  @Autowired
  private TemplateEngine templateEngine;

  @Autowired
  private JWTHelper jwtHelper;

  @Value("${app.public.url}")
  private String publicUrl;

  @Value("${app.timeout.verification}")
  private int timeoutVerificationSeconds;

  @Value("${app.timeout.access}")
  private int timeoutAccessSeconds;

  @Value("${app.custom.uri.prefix}")
  private String customUriPrefix;

  private UriComponentsBuilder publicUrlBuilder;

  @PostConstruct
  public void init() {

    // throws IllegalArgumentException on invalid URIs -> startup will fail if URL is invalid
    this.publicUrlBuilder = UriComponentsBuilder.fromUriString(this.publicUrl);
  }

  public Survey getReleasedSurvey(final String nameId) {

    return this.surveyRepository.findTopByNameIdAndReleaseStatusOrderByVersionDesc(nameId, ReleaseStatusType.RELEASED)
        .get();
  }

  public String verifyEmail(final VerificationDto verificationDto) throws IOException {

    final Optional<Verification> verificationOp =
        this.verificationRepository.findByHashAndVerified(verificationDto.getVerificationToken(), false);

    if (verificationOp.isEmpty())
      throw new IllegalArgumentException();

    final Verification verification = verificationOp.get();

    // Validate if verification is still valid

    final Instant instant =
        verification.getUpdatedAt() == null ? verification.getCreatedAt() : verification.getUpdatedAt();
    if (instant.plusSeconds(this.timeoutVerificationSeconds).isBefore(Instant.now())) {

      LOG.info("Expired email verification requested.");
      throw new IllegalArgumentException(); // keep silent about it
    }

    // Update verification - do not delete hash to avoid other users receiving the same hash later
    verification.setVerified(true);
    verification.setUpdatedAt(Instant.now());
    this.verificationRepository.save(verification);

    final Optional<User> userOp = this.userRepository.findByUserToken(verificationDto.getConfirmationToken());
    User user = null;

    final String newUserToken = generateString(TOKEN_CONFIRM_LENGTH);

    if (userOp.isEmpty()) {
      // Generate new User ID
      user = this.userRepository.save(User.builder().userToken(newUserToken).build());
    } else {
      final User existingUser = userOp.get();
      existingUser.setUserToken(newUserToken);
      user = this.userRepository.save(existingUser);
    }

    final String email = verification.getEmail();
    sendConfirmationEmail(email, user.getUserToken());

    return this.jwtHelper.createJWT(user.getId(), this.timeoutAccessSeconds);
  }

  public void registerParticipant(final RegistrationDto registration, final boolean autoUpdateInvitation)
      throws IOException {

    final String verificationToken = getValidVerificationToken();

    final Optional<Verification> verificationOp = this.verificationRepository.findByEmail(registration.getEmail());

    boolean continueInivitation = false;

    if (verificationOp.isEmpty()) {
      // add new entity
      final Verification verification = Verification.builder()
          .email(registration.getEmail())
          .hash(verificationToken)
          .verified(false)
          .build();
      this.verificationRepository.save(verification);
      continueInivitation = true;

    } else if (autoUpdateInvitation) {
      // update entity
      final Verification verification = verificationOp.get();
      verification.setUpdatedAt(Instant.now());
      verification.setVerified(false);
      verification.setHash(verificationToken);
      this.verificationRepository.save(verification);
      continueInivitation = true;
    }

    if (continueInivitation)
      sendRegistrationEmail(registration.getEmail(), verificationToken, registration.getConfirmationToken());
  }

  public void importParticipants(final InputStream inputStream) throws IOException {

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

      reader.lines().forEach(line -> {
        try {
          registerParticipant(RegistrationDto.builder().email(line).build(), false);
        } catch (final IOException e) {
          LOG.error("IMPORT: Unable to register participant: {}", line);
        }
      });
    }
  }

  public String handleVerificationRequest(final String verificationToken, final String userToken) {

    final String path =
        userToken == null || userToken.isBlank() ? verificationToken : verificationToken + "/" + userToken;

    final Context context = new Context();
    context.setVariable("customURI", this.customUriPrefix + "://verify/" + path);

    return this.templateEngine.process("verifyTemplate", context);
  }

  /**
   *
   * @return
   */
  private String getValidVerificationToken() {

    final String hash = generateString(TOKEN_VERIFY_LENGTH);
    if (this.verificationRepository.existsByHash(hash))
      return getValidVerificationToken(); // repeat

    return hash;
  }

  /**
   *
   * @param length
   * @return
   */
  private String generateString(final int length) {
    final int leftLimit = 48; // numeral '0'
    final int rightLimit = 122; // letter 'z'

    final String generatedString = RANDOM.ints(leftLimit, rightLimit + 1)
        .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
        .limit(length)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();

    return generatedString;
  }

  /**
   * @param email
   * @param verificationToken
   * @throws IOException
   */
  private void sendRegistrationEmail(final String email, final String verificationToken, final String userToken)
      throws IOException {

    final UriComponentsBuilder builder = this.publicUrlBuilder.cloneBuilder()
        .path("/verify")
        .queryParam("token", verificationToken);

    if (userToken != null && !userToken.isBlank())
      builder.queryParam("userToken", userToken);

    final String publicLink = builder
        .build()
        .encode()
        .toString();

    LOG.debug("Sending email to '{}' with verification link: '{}'", email, publicLink);

    final Context context = new Context();
    context.setVariable("link", publicLink);
    final String message = this.templateEngine.process("registrationTemplate", context);
    final boolean success = this.emailService.sendHTML(email, "Registration", message);

    if (!success)
      throw new IOException("Sending email to recipient '" + email + "' was not successful.");
  }

  /**
   *
   * @param email
   * @param hash
   * @throws IOException
   */
  private void sendConfirmationEmail(final String email, final String hash) throws IOException {

    LOG.debug("Sending email to '{}' with confirmation token: '{}'", email, hash);

    final Context context = new Context();
    context.setVariable("token", hash);
    final String message = this.templateEngine.process("confirmationTemplate", context);
    final boolean success = this.emailService.sendHTML(email, "Confirmation", message);

    if (!success)
      throw new IOException("Sending email to recipient '" + email + "' was not successful.");
  }

  /**
   * TODO: Currently the interval logic is very limited and needs to be implemented as a generic
   * approach later.
   *
   * @return
   */
  public Collection<SurveyStatusDto> getSurveyOverview(final String userId) {

    final User user = this.userRepository.findById(userId).get();

    final Map<String, SurveyStatusDto> result = new HashMap<>();

    for (final Survey survey : this.surveyRepository
        .findAllByReleaseStatusOrderByNameIdAscVersionDesc(ReleaseStatusType.RELEASED)) {

      // Collect each survey only once by its top most released version
      if (result.get(survey.getNameId()) != null)
        continue;

      final SurveyInstance instance = getCurrentInstance(survey);

      final Optional<SurveyStatus> surveyStatusOp =
          this.surveyStatusRepository.findByUserAndSurveyInstance(user, instance);

      Long nextQuestionId = null;

      if (surveyStatusOp.isPresent()) {
        final SurveyStatus surveyStatus = surveyStatusOp.get();
        nextQuestionId = surveyStatus.getNextQuestion() == null ? null : surveyStatus.getNextQuestion().getId();
      }

      final SurveyStatusType status = calculateSurveyStatus(user, instance);

      result.put(survey.getNameId(),
          SurveyStatusDto.builder()
              .nameId(survey.getNameId())
              .status(status)
              .title(survey.getTitle())
              .description(survey.getDescription())
              .countQuestions(survey.getQuestions().size())
              .nextQuestionId(nextQuestionId)
              .token(instance.getToken())
              .startTime(INSTANT_MIN.equals(instance.getStartTime()) ? null : instance.getStartTime().toEpochMilli())
              .endTime(INSTANT_MAX.equals(instance.getEndTime()) ? null : instance.getEndTime().toEpochMilli())
              .build());
    }

    return result.values();
  }

  private SurveyStatusType calculateSurveyStatus(final User user, final SurveyInstance instance) {

    final Map<Long, SurveyResponse> responses =
        this.surveyResponseRepository.findByUserAndSurveyInstance(user, instance)
            .stream().collect(Collectors.toMap(
                e -> e.getQuestion().getId(),
                e -> e));

    if (responses.isEmpty())
      return SurveyStatusType.INCOMPLETE;

    if (checkAnswers(instance.getSurvey().getQuestions(), responses))
      return SurveyStatusType.COMPLETED;

    return SurveyStatusType.INCOMPLETE;
  }

  private boolean checkAnswers(final List<Question> questions, final Map<Long, SurveyResponse> responses) {

    if (questions == null || responses == null || responses.isEmpty())
      return false;

    for (final Question question : questions) {

      if (!isAnswered(question, responses))
        return false;

      if (isSubQuestionRequired(question, responses.get(question.getId()))) {
        if (!checkAnswers(getQuestions(question), responses))
          return false;
      }
    }

    return true;
  }

  private List<Question> getQuestions(final Question question) {

    switch (question.getType()) {
      case BOOL:
        return ((BooleanQuestion) question).getContainer().getQuestions();
      case CHOICE:
        return ((ChoiceQuestion) question).getContainer().getQuestions();
      default:
        return null;

    }
  }

  /**
   *
   * @param question
   * @param response
   * @return
   */
  private boolean isSubQuestionRequired(final Question question, final SurveyResponse response) {

    if (question == null || response == null || !question.hasContainer())
      return false;

    switch (question.getType()) {
      case BOOL:

        final BooleanQuestion booleanQuestion = (BooleanQuestion) question;
        if (booleanQuestion.getContainer().getDependsOn() == null)
          return true;

        return response.getBoolAnswer().equals(booleanQuestion.getContainer().getDependsOn());

      case CHOICE:

        final ChoiceQuestion choiceQuestion = (ChoiceQuestion) question;
        if (choiceQuestion.getContainer().getDependsOn() == null)
          return true;

        final List<Long> givenAnswerIds =
            response.getAnswers().stream().map(Answer::getId).collect(Collectors.toList());

        // anyMatch -> OR-linked answers
        // allMatch -> AND-linked answers
        return choiceQuestion.getContainer().getDependsOn().stream().anyMatch(p -> givenAnswerIds.contains(p.getId()));

      default:
        return true;

    }
  }

  private boolean isAnswered(final Question question, final Map<Long, SurveyResponse> responses) {

    switch (question.getType()) {
      case BOOL: {

        final SurveyResponse response = responses.get(question.getId());
        return response != null && response.getBoolAnswer() != null;

      }
      case CHECKLIST: {

        final ChecklistQuestion checklistQuestion = (ChecklistQuestion) question;
        for (final ChecklistEntry entry : checklistQuestion.getEntries()) {

          final SurveyResponse response = responses.get(entry.getId());
          if (response == null || response.getBoolAnswer() == null)
            return false;
        }

        return true;

      }
      case CHOICE: {

        final ChoiceQuestion choiceQuestion = (ChoiceQuestion) question;
        final SurveyResponse response = responses.get(question.getId());

        if (response == null || response.getAnswers() == null || response.getAnswers().isEmpty())
          return false;

        for (final Answer answer : choiceQuestion.getAnswers()) {
          if (response.getAnswers().stream().anyMatch(p -> p.getId().equals(answer.getId())))
            return true;
        }

        return false;

      }
      case RANGE: {

        final SurveyResponse response = responses.get(question.getId());
        return response != null && response.getRangeAnswer() != null;

      }
      case TEXT: {

        final SurveyResponse response = responses.get(question.getId());
        return response != null && response.getTextAnswer() != null && !response.getTextAnswer().isBlank();

      }
      default:
        return false;
    }
  }

  /**
   * Self-healing implementation: If the current survey instance does not yet exist, it will be
   * created on request.
   *
   * @param survey
   * @return
   */
  private SurveyInstance getCurrentInstance(final Survey survey) {

    switch (survey.getIntervalType()) {
      case WEEK:
        return getWeeklyInstance(survey);
      case NONE:
      default:
        return getPermanentInstance(survey);
    }
  }

  private SurveyInstance getWeeklyInstance(final Survey survey) {

    final Instant startOfWeek = ZonedDateTime.now(ZoneOffset.UTC)
        .with(TemporalAdjusters.previous(DayOfWeek.MONDAY))
        .truncatedTo(ChronoUnit.DAYS)
        .toInstant();

    final Instant endOfWeek = ZonedDateTime.now(ZoneOffset.UTC)
        .with(TemporalAdjusters.next(DayOfWeek.MONDAY))
        .truncatedTo(ChronoUnit.DAYS)
        .minusSeconds(1).toInstant();

    return getInstance(survey, startOfWeek, endOfWeek);
  }

  private SurveyInstance getPermanentInstance(final Survey survey) {

    return getInstance(survey, INSTANT_MIN, INSTANT_MAX);
  }

  private SurveyInstance getInstance(final Survey survey, final Instant min, final Instant max) {

    final Optional<SurveyInstance> instanceOp =
        this.surveyInstanceRepository.findBySurveyAndStartTimeAndEndTime(survey, min, max);

    if (instanceOp.isPresent())
      return instanceOp.get();

    return this.surveyInstanceRepository.save(SurveyInstance.builder()
        .survey(survey)
        .startTime(min)
        .endTime(max)
        .token(generateString(TOKEN_SURVEY_LENGTH))
        .build());
  }
}
