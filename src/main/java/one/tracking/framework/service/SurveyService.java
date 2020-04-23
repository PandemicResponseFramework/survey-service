/**
 *
 */
package one.tracking.framework.service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.Verification;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.container.BooleanContainer;
import one.tracking.framework.entity.meta.container.ChoiceContainer;
import one.tracking.framework.entity.meta.container.DefaultContainer;
import one.tracking.framework.entity.meta.question.BooleanQuestion;
import one.tracking.framework.entity.meta.question.ChoiceQuestion;
import one.tracking.framework.entity.meta.question.Question;
import one.tracking.framework.entity.meta.question.RangeQuestion;
import one.tracking.framework.entity.meta.question.TextQuestion;
import one.tracking.framework.entity.meta.question.TitleQuestion;
import one.tracking.framework.repo.AnswerRepository;
import one.tracking.framework.repo.ContainerRepository;
import one.tracking.framework.repo.QuestionRepository;
import one.tracking.framework.repo.SurveyRepository;
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

  @Autowired
  private AnswerRepository answerRepository;

  @Autowired
  private QuestionRepository questionRepository;

  @Autowired
  private ContainerRepository containerRepository;

  @Autowired
  private SurveyRepository surveyRepository;

  @Autowired
  private VerificationRepository verificationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private SendGridService emailService;

  @Autowired
  private TemplateEngine templateEngine;

  @Autowired
  private JWTHelper jwtHelper;

  @Value("${app.public.url}")
  private String publicUrl;

  @Value("${app.verification.timeout}")
  private int verificationTimeoutSeconds;

  private UriComponentsBuilder publicUrlBuilder;

  @PostConstruct
  public void init() {

    // throws IllegalArgumentException on invalid URIs -> startup will fail if URL is invalid
    this.publicUrlBuilder = UriComponentsBuilder.fromUriString(this.publicUrl);
  }

  public Survey getSurvey(final String nameId) {

    return this.surveyRepository.findByNameId(nameId).get();
  }

  public String verifyEmail(final String hash) {

    final Optional<Verification> verificationOp = this.verificationRepository.findByHashAndVerified(hash, false);

    if (verificationOp.isEmpty())
      return null;

    final Verification verification = verificationOp.get();

    // Validate if verification is still valid

    final Instant instant =
        verification.getUpdatedAt() == null ? verification.getCreatedAt() : verification.getUpdatedAt();
    if (instant.plusSeconds(this.verificationTimeoutSeconds).isAfter(Instant.now())) {

      LOG.info("Expired email verification requested.");
      throw new IllegalArgumentException(); // keep silence about it
    }

    // Update verification

    verification.setVerified(true);
    verification.setUpdatedAt(Instant.now());
    this.verificationRepository.save(verification);

    // TODO: send confirmation email maybe?
    // final String email = verificationOp.get().getEmail();

    // Generate new User ID
    final User user = this.userRepository.save(User.builder().build());

    return this.jwtHelper.createJWT(user.getId(), -1);
  }

  public void registerParticipant(final String email) throws IOException {

    final String hash = getValidHash();

    final Optional<Verification> verificationOp = this.verificationRepository.findByEmail(email);

    if (verificationOp.isEmpty()) {
      // add new entity
      final Verification verification = Verification.builder()
          .email(email)
          .hash(hash)
          .verified(false)
          .build();
      this.verificationRepository.save(verification);

    } else {
      // update entity
      final Verification verification = verificationOp.get();
      verification.setUpdatedAt(Instant.now());
      verification.setVerified(false);
      verification.setHash(hash);
      this.verificationRepository.save(verification);

    }

    final String publicLink = this.publicUrlBuilder.path("/verify").path("/" + hash).build().encode().toString();
    final Context context = new Context();
    context.setVariable("link", publicLink);
    final String message = this.templateEngine.process("registrationTemplate", context);
    this.emailService.sendHTML(email, "Registration", message);
  }

  /**
   *
   * @return
   */
  private String getValidHash() {

    final String hash = UUID.randomUUID().toString();
    if (this.verificationRepository.existsByHash(hash))
      return getValidHash(); // repeat

    return hash;
  }

  /**
   * FIXME DEV only
   *
   * @param event
   */
  @EventListener
  private void handleEvent(final ApplicationStartedEvent event) {

    createBasicSurvey();
    createRegularSurvey();
  }

  /**
   * FIXME DEV only
   */
  private void createBasicSurvey() {

    int order = 1;
    final List<Question> questions = new ArrayList<>(12);

    // 1
    questions.add(createChoiceQuestion(
        "What is your age?",
        order++, false,
        Arrays.asList(
            "18 - 24",
            "25 - 44",
            "45 - 64",
            "65 +")));

    // 2
    questions.add(createChoiceQuestion(
        "Gender?",
        order++, false,
        Arrays.asList(
            "Male",
            "Female")));

    // 3
    questions.add(createChoiceQuestion(
        "What is your marital status?",
        order++, false,
        Arrays.asList(
            "Married or civil partnership",
            "Living with partner",
            "Single")));

    // 4
    questions.add(createChoiceQuestion(
        "Are you in employment?",
        order++, false,
        Arrays.asList(
            "Full time employment",
            "Part time employment",
            "Retired",
            "Not in employment"),
        Arrays.asList(
            "Full time employment",
            "Part time employment"),
        Collections.singletonList(createTextQuestion("Type of employment:", 1, false))));

    // 5
    questions.add(createChoiceQuestion(
        "In general, how do you find your job?",
        order++, false,
        Arrays.asList(
            "Not at all stressful",
            "Mildly stressful",
            "Moderately stressful",
            "Very stressful",
            "Extremely stressful")));

    // 6
    questions.add(createBoolQuestion(
        "Did you get a flu vaccination in the past year?", order++));

    // 7
    questions.add(createBoolQuestion(
        "In general, do you have any health problems that require you to limit your activities?", order++,
        true,
        Collections
            .singletonList(createTextQuestion("If yes, what health problem limits your activities?", 1, false))));

    // 8
    questions.add(createBoolQuestion(
        "Do you suffer from heart disease?", order++, null, null));

    // 9
    questions.add(createBoolQuestion(
        "Do you suffer from diabetes?", order++, null, null));

    // 10
    questions.add(createBoolQuestion(
        "Do you suffer from lung disease / asthma?", order++, null, null));

    // 11
    questions.add(createBoolQuestion(
        "Do you suffer from kidney disease?", order++, null, null));

    // 12
    questions.add(createBoolQuestion(
        "Do you smoke?", order++, null, null));

    this.surveyRepository.save(Survey.builder()
        .questions(questions)
        .nameId("BASIC")
        .build());
  }

  /**
   * FIXME DEV only
   */
  private void createRegularSurvey() {

    int order = 1;
    final List<Question> questions = new ArrayList<>(12);

    // 13
    questions.add(createChoiceQuestion(
        "Under each heading, please tick the ONE box that best describes your health TODAY:",
        order++, false,
        Arrays.asList(
            "I have no problems walking about",
            "I have some problems walking about",
            "I have a lot of problems walking about")));

    // 14
    questions.add(createChoiceQuestion(
        "Looking after myself, please tick the ONE box that best describes your health TODAY:",
        order++, false,
        Arrays.asList(
            "I have no problems washing or dressing myself",
            "I have some problems washing or dressing myself",
            "I have a lot of problems washing or dressing myself")));

    // 15
    questions.add(createChoiceQuestion(
        "Doing usual activities please tick the ONE box that best describes your health TODAY (for example, work, study, housework, family or leisure activities):",
        order++, false,
        Arrays.asList(
            "I have no problems doing my usual activities",
            "I have some problems doing my usual activities",
            "I have a lot of problems doing my usual activities")));

    // 16
    questions.add(createChoiceQuestion(
        "Having pain/ discomfort:",
        order++, false,
        Arrays.asList(
            "I have no pain or discomfort",
            "I have some pain or discomfort",
            "I have a lot of pain or discomfort")));

    // 17
    questions.add(createChoiceQuestion(
        "Feeling worried, sad or unhappy:",
        order++, false,
        Arrays.asList(
            "I am not worried, sad or unhappy",
            "I am a bit worried, sad or unhappy",
            "I am very worries, sad or unhappy")));

    // 18
    questions.add(createChoiceQuestion(
        "How often do you feel lonely?",
        order++, false,
        Arrays.asList(
            "Often / always",
            "Some of the time",
            "Occasionally",
            "Hardly ever",
            "Never")));

    // 19
    questions.add(createChoiceQuestion(
        "How would you rate your general health TODAY:",
        order++, false,
        Arrays.asList(
            "Excellent",
            "Very good",
            "Good",
            "Fair",
            "Poor")));

    // 20
    questions.add(createBoolQuestion(
        "Do you think you currently have/ had a COVID-19 infection?",
        order++));

    // 21
    questions.add(createBoolQuestion(
        "Have you been tested for a COVID-19 infection?",
        order++, null, null));

    // 22
    questions.add(createBoolQuestion(
        "Have you been hospitalised for a COVID-19 infection?",
        order++,
        true,
        Collections.singletonList(createTextQuestion("How many days were you sick?", 1, false))));

    // 23
    questions.add(createTitleQuestion(
        "To what degree have you experienced the following symptoms in the last 7 days:",
        order++,
        Arrays.asList(
            createBoolQuestion("Headache", 1),
            createBoolQuestion("Muscle pain/aches", 2),
            createBoolQuestion("Difficulty breathing", 3),
            createBoolQuestion("Fever/ high temperature", 4),
            createBoolQuestion("Sore throat", 5),
            createBoolQuestion("Dry cough", 6),
            createBoolQuestion("Wet cough", 7),
            createBoolQuestion("I felt physically exhausted", 8),
            createBoolQuestion("loss of smell and taste", 9))));

    // 24
    questions.add(createRangeQuestion(
        "How much have you been concerned about the corona crisis in the past 7 days?",
        order++,
        1, 10,
        null,
        "Not concerned", "Extremely concerned"));

    // 25
    questions.add(createRangeQuestion(
        "How would you rate your quality of life over the last 7 days?",
        order++,
        1, 10,
        null,
        "Terrible", "Outstanding"));

    // 26
    questions.add(createRangeQuestion(
        "How socially isolated have you felt in the last 7 days?",
        order++,
        1, 10,
        null,
        "Not socially isolated", "Extremely socially isolated"));

    // 27
    questions.add(createRangeQuestion(
        "If you suffer from depression, how much has this condition been affected by the COVID-19 crisis in the last 7 days?",
        order++,
        1, 10,
        null,
        "Not at all", "Extremely depressed"));

    // 28
    questions.add(createRangeQuestion(
        "If you suffer from anxiety, how much has this condition been affected by the COVID-19 crisis in the last 7 days?",
        order++,
        1, 10,
        null,
        "Not at all", "Extremely anxious"));

    // 29
    questions.add(createChoiceQuestion(
        "How would you rate your overall mental health?",
        order++, false,
        Arrays.asList(
            "Excellent",
            "Very good",
            "Good",
            "Fair",
            "Poor")));

    // 30
    questions.add(createBoolQuestion(
        "Has the COVID-19 crisis affected the way you normally source your food/ medicines?",
        order++));

    // 31
    questions.add(createChoiceQuestion(
        "Where do you currently shop for your groceries?",
        order++, false,
        Arrays.asList(
            "Local supermarket",
            "Online shopping",
            "Local corner shops",
            "Farmer’s market")));

    // 32
    questions.add(createChoiceQuestion(
        "How healthy is your eating pattern compared to the period before the COVID-19 crisis?",
        order++, false,
        Arrays.asList(
            "Much less healthy",
            "Just as healthy",
            "Healthier")));

    // 33
    questions.add(createChoiceQuestion(
        "How would you rate your overall diet?",
        order++, false,
        Arrays.asList(
            "Excellent",
            "Very good",
            "Good",
            "Fair",
            "Poor")));

    // 34
    questions.add(createChoiceQuestion(
        "How many hours of moderately intense exercise did you do before the Covid-19 crisis?",
        order++, false,
        Arrays.asList(
            "Less than 1 hour",
            "1-2 hours",
            "More than 2 hours")));

    // 35
    questions.add(createBoolQuestion(
        "Has the COVID-19 crisis affected your ability to exercise?",
        order++,
        true,
        Collections.singletonList(createTextQuestion("If yes, how?", 1, false))));

    // 36
    questions.add(createTextQuestion(
        "Has the COVID-19 crisis changed the way that you use social media?",
        order++, true));

    this.surveyRepository.save(Survey.builder()
        .questions(questions)
        .nameId("REGULAR")
        .build());
  }

  /**
   *
   * @param answer
   * @return
   */
  private Answer createAnswer(final String answer) {
    return this.answerRepository.save(Answer.builder()
        .value(answer)
        .build());
  }

  private Question createChoiceQuestion(
      final String question,
      final int order,
      final boolean multiple,
      final List<String> answers) {

    return createChoiceQuestion(question, order, multiple, answers, null, null);
  }

  /**
   *
   * @param question
   * @param order
   * @param multiple
   * @param answers
   * @param dependsOn
   * @param subQuestions
   * @return
   */
  private Question createChoiceQuestion(
      final String question,
      final int order,
      final boolean multiple,
      final List<String> answers,
      final List<String> dependsOn,
      final List<Question> subQuestions) {

    if (answers == null || answers.isEmpty())
      throw new IllegalArgumentException("Answers must not be null or empty.");

    final List<Answer> answerEntities = answers.stream().map(f -> createAnswer(f)).collect(Collectors.toList());

    ChoiceContainer container = null;

    if (subQuestions != null && !subQuestions.isEmpty()) {

      final List<Answer> dependsOnAnswers = dependsOn == null || dependsOn.isEmpty() ? null
          : answerEntities.stream().filter(p -> dependsOn.contains(p.getValue())).collect(Collectors.toList());

      container = this.containerRepository.save(ChoiceContainer.builder()
          .dependsOn(dependsOnAnswers)
          .subQuestions(subQuestions)
          .build());
    }

    return this.questionRepository.save(ChoiceQuestion.builder()
        .question(question)
        .ranking(order)
        .container(container)
        .answers(answerEntities)
        .multiple(multiple)
        .build());
  }

  /**
   *
   * @param question
   * @param order
   * @return
   */
  private Question createBoolQuestion(
      final String question,
      final int order) {

    return createBoolQuestion(question, order, null, null);
  }

  /**
   *
   * @param question
   * @param order
   * @param dependsOn
   * @param subQuestions
   * @return
   */
  private Question createBoolQuestion(
      final String question,
      final int order,
      final Boolean dependsOn,
      final List<Question> subQuestions) {

    BooleanContainer container = null;

    if (subQuestions != null && !subQuestions.isEmpty()) {

      container = this.containerRepository.save(BooleanContainer.builder()
          .subQuestions(subQuestions)
          .dependsOn(dependsOn)
          .build());
    }

    return this.questionRepository.save(BooleanQuestion.builder()
        .question(question)
        .container(container)
        .ranking(order)
        .build());
  }

  /**
   *
   * @param question
   * @param order
   * @return
   */
  private Question createTextQuestion(
      final String question,
      final int order,
      final boolean multiline) {

    return createTextQuestion(question, order, multiline, null);
  }

  /**
   *
   * @param question
   * @param order
   * @param subQuestions
   * @return
   */
  private Question createTextQuestion(
      final String question,
      final int order,
      final boolean multiline,
      final List<Question> subQuestions) {

    DefaultContainer container = null;

    if (subQuestions != null && !subQuestions.isEmpty()) {

      container = this.containerRepository.save(DefaultContainer.builder()
          .subQuestions(subQuestions)
          .build());
    }

    return this.questionRepository.save(TextQuestion.builder()
        .question(question)
        .multiline(multiline)
        .container(container)
        .ranking(order)
        .build());
  }

  /**
   *
   * @param question
   * @param order
   * @param subQuestions
   * @return
   */
  private Question createTitleQuestion(
      final String question,
      final int order,
      final List<Question> subQuestions) {

    if (subQuestions == null || subQuestions.isEmpty())
      throw new IllegalArgumentException("Sub questions must not be null or empty.");

    final DefaultContainer container = this.containerRepository.save(DefaultContainer.builder()
        .subQuestions(subQuestions)
        .build());

    return this.questionRepository.save(TitleQuestion.builder()
        .question(question)
        .container(container)
        .ranking(order)
        .build());
  }

  /**
   *
   * @param question
   * @param order
   * @param minValue
   * @param maxValue
   * @param defaultValue
   * @param minText
   * @param maxText
   * @return
   */
  private Question createRangeQuestion(
      final String question,
      final int order,
      final int minValue, final int maxValue,
      final Integer defaultValue,
      final String minText, final String maxText) {

    return createRangeQuestion(question, order, minValue, maxValue, defaultValue, minText, maxText, null);
  }

  /**
   *
   * @param question
   * @param order
   * @param subQuestions
   * @return
   */
  private Question createRangeQuestion(
      final String question,
      final int order,
      final int minValue, final int maxValue,
      final Integer defaultValue,
      final String minText, final String maxText,
      final List<Question> subQuestions) {

    DefaultContainer container = null;

    if (subQuestions != null && !subQuestions.isEmpty()) {

      container = this.containerRepository.save(DefaultContainer.builder()
          .subQuestions(subQuestions)
          .build());
    }

    return this.questionRepository.save(RangeQuestion.builder()
        .question(question)
        .minValue(minValue)
        .maxValue(maxValue)
        .defaultValue(defaultValue)
        .minText(minText)
        .maxText(maxText)
        .container(container)
        .ranking(order)
        .build());
  }
}
