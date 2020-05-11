/**
 *
 */
package one.tracking.framework.service;

import static one.tracking.framework.entity.DataConstants.TOKEN_CONFIRM_LENGTH;
import static one.tracking.framework.entity.DataConstants.TOKEN_VERIFY_LENGTH;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import one.tracking.framework.dto.RegistrationDto;
import one.tracking.framework.dto.SurveyResponseDto;
import one.tracking.framework.dto.VerificationDto;
import one.tracking.framework.dto.meta.question.QuestionType;
import one.tracking.framework.entity.SurveyResponse;
import one.tracking.framework.entity.SurveyResponse.SurveyResponseBuilder;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.Verification;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.question.ChecklistQuestion;
import one.tracking.framework.entity.meta.question.ChoiceQuestion;
import one.tracking.framework.entity.meta.question.Question;
import one.tracking.framework.entity.meta.question.RangeQuestion;
import one.tracking.framework.entity.meta.question.TextQuestion;
import one.tracking.framework.repo.AnswerRepository;
import one.tracking.framework.repo.QuestionRepository;
import one.tracking.framework.repo.SurveyRepository;
import one.tracking.framework.repo.SurveyResponseRepository;
import one.tracking.framework.repo.UserRepository;
import one.tracking.framework.repo.VerificationRepository;
import one.tracking.framework.util.JWTHelper;

/**
 * @author Marko Voß
 *
 */
@Service
public class SurveyService {

  private static final Logger LOG = LoggerFactory.getLogger(SurveyService.class);

  private static final Random RANDOM = new Random();

  @Autowired
  private AnswerRepository answerRepository;

  @Autowired
  private QuestionRepository questionRepository;

  @Autowired
  private SurveyRepository surveyRepository;

  @Autowired
  private VerificationRepository verificationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private SurveyResponseRepository surveyResponseRepository;

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

  public Survey getSurvey(final String nameId) {

    return this.surveyRepository.findByNameId(nameId).get();
  }

  @Transactional
  public void handleSurveyResponse(final String userId, final String nameId, final SurveyResponseDto surveyResponse) {

    final User user = this.userRepository.findById(userId).get();
    final Survey survey = this.surveyRepository.findByNameId(nameId).get();
    final Question question = this.questionRepository.findById(surveyResponse.getQuestionId()).get();
    final QuestionType type = QuestionType.valueOf(question.getType());

    if (!validateResponse(question, type, surveyResponse))
      throw new IllegalArgumentException("Invalid Survey Response.");

    this.surveyResponseRepository.deleteByUserAndSurveyAndQuestion(user, survey, question);

    final SurveyResponseBuilder entityBuilder = SurveyResponse.builder()
        .boolAnswer(surveyResponse.getBoolAnswer())
        .question(question)
        .survey(survey)
        .user(user);

    switch (type) {
      case BOOL:
        this.surveyResponseRepository.save(entityBuilder.boolAnswer(surveyResponse.getBoolAnswer()).build());
        break;
      case CHOICE:
        final List<Answer> existingAnswers = new ArrayList<>();

        for (final Long answerId : surveyResponse.getAnswerIds()) {
          final Optional<Answer> answerOp = this.answerRepository.findById(answerId);
          if (answerOp.isEmpty())
            throw new IllegalStateException("Unexpected state: Could not find answer entity for id: " + answerId);
          existingAnswers.add(answerOp.get());
        }

        this.surveyResponseRepository.save(entityBuilder.answers(existingAnswers).build());

        break;
      case RANGE:
        this.surveyResponseRepository.save(entityBuilder.rangeAnswer(surveyResponse.getRangeAnswer()).build());
        break;
      case TEXT:
        this.surveyResponseRepository.save(entityBuilder.textAnswer(surveyResponse.getTextAnswer()).build());
        break;
      default:
        // nothing
        break;
    }
  }

  private final boolean validateBoolResponse(final Question question, final SurveyResponseDto response) {

    return response.getBoolAnswer() != null;
  }

  private final boolean validateTextResponse(final Question question, final SurveyResponseDto response) {

    return response.getTextAnswer() != null && !response.getTextAnswer().isBlank()
        && response.getTextAnswer().length() <= ((TextQuestion) question).getLength();
  }

  private final boolean validateChoiceResponse(final Question question, final SurveyResponseDto response) {

    if (response.getAnswerIds() == null || response.getAnswerIds().isEmpty())
      return false;

    final ChoiceQuestion choiceQuestion = (ChoiceQuestion) question;

    if (choiceQuestion.getMultiple() == false && response.getAnswerIds().size() > 1)
      return false;

    final List<Long> originAnswerIds =
        choiceQuestion.getAnswers().stream().map(Answer::getId).collect(Collectors.toList());

    // Does modify the response object but the request will be denied if modification occurred
    return !response.getAnswerIds().retainAll(originAnswerIds);
  }

  private final boolean validateRangeResponse(final Question question, final SurveyResponseDto response) {

    // TODO: introduce step
    final RangeQuestion rangeQuestion = (RangeQuestion) question;
    return response.getRangeAnswer() != null
        && response.getRangeAnswer() >= rangeQuestion.getMinValue()
        && response.getRangeAnswer() <= rangeQuestion.getMaxValue();
  }

  private boolean validateChecklistResponse(final Question question, final SurveyResponseDto response) {

    if (response.getChecklistAnswer() == null || response.getChecklistAnswer().isEmpty())
      return false;

    final ChecklistQuestion checklistQuestion = (ChecklistQuestion) question;
    final List<Long> originQuestionIds =
        checklistQuestion.getEntries().stream().map(Question::getId).collect(Collectors.toList());

    // Does modify the response object but the request will be denied if modification occurred
    return !response.getChecklistAnswer().keySet().retainAll(originQuestionIds);
  }

  private boolean validateResponse(final Question question, final QuestionType type, final SurveyResponseDto response) {

    switch (type) {
      case BOOL:
        return validateBoolResponse(question, response);
      case CHOICE:
        return validateChoiceResponse(question, response);
      case RANGE:
        return validateRangeResponse(question, response);
      case TEXT:
        return validateTextResponse(question, response);
      case CHECKLIST:
        return validateChecklistResponse(question, response);
      default:
        return false;
    }
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
      throw new IOException("Sending email to receipient '" + email + "' was not successful.");
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
      throw new IOException("Sending email to receipient '" + email + "' was not successful.");
  }
}
