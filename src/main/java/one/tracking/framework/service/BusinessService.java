/**
 *
 */
package one.tracking.framework.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.BooleanQuestion;
import one.tracking.framework.entity.meta.ChoiceQuestion;
import one.tracking.framework.entity.meta.Question;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.repo.AnswerRepository;
import one.tracking.framework.repo.QuestionRepository;
import one.tracking.framework.repo.SurveyRepository;

/**
 * @author Marko Voß
 *
 */
@Service
public class BusinessService {

  @Autowired
  private AnswerRepository answerRepository;

  @Autowired
  private QuestionRepository questionRepository;

  @Autowired
  private SurveyRepository surveyRepository;

  @EventListener
  private void handleEvent(final ApplicationStartedEvent event) {

    createBasicSurvey();
  }

  /**
   *
   */
  private void createBasicSurvey() {

    int order = 1;
    final List<Question> questions = new ArrayList<>(12);

    questions.add(createChoiceQuestion(
        "What is your age?", order++, false,
        "18 - 24",
        "25 - 44",
        "45 - 64",
        "65 +"));

    questions.add(createChoiceQuestion(
        "Gender?", order++, false,
        "Male",
        "Female"));

    questions.add(createChoiceQuestion(
        "What is your marital status?", order++, false,
        "Married or civil partnership",
        "Living with partner",
        "Single"));

    // TODO SUB
    questions.add(createChoiceQuestion(
        "Are you in employment?", order++, false,
        "Full time employment",
        "Part time employment",
        "Retired",
        "Not in employment"));

    questions.add(createChoiceQuestion(
        "In general, how do you find your job?", order++, false,
        "Not at all stressful",
        "Mildly stressful",
        "Moderately stressful",
        "Very stressful",
        "Extremely stressful"));

    questions.add(createBoolQuestion(
        "Did you get a flu vaccination in the past year?", order++));

    // TODO SUB
    questions.add(createBoolQuestion(
        "In general, do you have any health problems that require you to limit your activities?", order++));

    questions.add(createBoolQuestion(
        "Do you suffer from heart disease?", order++));

    questions.add(createBoolQuestion(
        "Do you suffer from diabetes?", order++));

    questions.add(createBoolQuestion(
        "Do you suffer from lung disease / asthma?", order++));

    questions.add(createBoolQuestion(
        "Do you suffer from kidney disease?", order++));

    questions.add(createBoolQuestion(
        "Do you smoke?", order++));

    this.surveyRepository.save(Survey.builder()
        .questions(questions)
        .nameId("BASIC")
        .build()).getId();
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

  /**
   *
   * @param question
   * @param order
   * @param multiple
   * @param answers
   * @return
   */
  private Question createChoiceQuestion(final String question, final int order, final boolean multiple,
      final String... answers) {

    return this.questionRepository.save(ChoiceQuestion.builder()
        .question(question)
        .ranking(order)
        .answers(Arrays.stream(answers).map(f -> createAnswer(f)).collect(Collectors.toList()))
        .multiple(multiple)
        .build());
  }

  /**
   *
   * @param question
   * @param order
   * @return
   */
  private Question createBoolQuestion(final String question, final int order) {

    return this.questionRepository.save(BooleanQuestion.builder()
        .question(question)
        .ranking(order)
        .build());
  }
}
