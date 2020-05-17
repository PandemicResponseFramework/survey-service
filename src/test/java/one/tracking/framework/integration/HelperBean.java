/**
 *
 */
package one.tracking.framework.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.IntervalType;
import one.tracking.framework.entity.meta.ReleaseStatusType;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.container.BooleanContainer;
import one.tracking.framework.entity.meta.container.ChoiceContainer;
import one.tracking.framework.entity.meta.container.DefaultContainer;
import one.tracking.framework.entity.meta.question.BooleanQuestion;
import one.tracking.framework.entity.meta.question.ChecklistEntry;
import one.tracking.framework.entity.meta.question.ChecklistQuestion;
import one.tracking.framework.entity.meta.question.ChoiceQuestion;
import one.tracking.framework.entity.meta.question.Question;
import one.tracking.framework.entity.meta.question.RangeQuestion;
import one.tracking.framework.entity.meta.question.TextQuestion;
import one.tracking.framework.repo.AnswerRepository;
import one.tracking.framework.repo.ContainerRepository;
import one.tracking.framework.repo.QuestionRepository;
import one.tracking.framework.repo.SurveyRepository;

/**
 * @author Marko Voß
 *
 */
public class HelperBean {

  @Autowired
  private AnswerRepository answerRepository;

  @Autowired
  private QuestionRepository questionRepository;

  @Autowired
  private ContainerRepository containerRepository;

  @Autowired
  private SurveyRepository surveyRepository;

  public void createTestSurvey() {

    int order = 0;
    final List<Question> questions = new ArrayList<>(12);

    // boolean - no children
    questions.add(createBoolQuestion(
        "Q1", order++));

    // boolean - with children
    questions.add(createBoolQuestion(
        "Q2", order++,
        true,
        Collections.singletonList(createBoolQuestion("Q2C1", 0))));

    // single choice - no children
    questions.add(createChoiceQuestion(
        "Q3",
        order++, false,
        Arrays.asList(
            "Q3A1",
            "Q3A2",
            "Q3A3")));

    // multiple choice - no children
    questions.add(createChoiceQuestion(
        "Q4",
        order++, true,
        Arrays.asList(
            "Q4A1",
            "Q4A2",
            "Q4A3")));


    // single choice - with children
    questions.add(createChoiceQuestion(
        "Q5",
        order++, false,
        Arrays.asList(
            "Q5A1",
            "Q5A2",
            "Q5A3"),
        Arrays.asList(
            "Q5A1",
            "Q5A2"),
        Collections.singletonList(createBoolQuestion("Q5C1", 0))));

    // multiple choice - with children
    questions.add(createChoiceQuestion(
        "Q6",
        order++, true,
        Arrays.asList(
            "Q6A1",
            "Q6A2",
            "Q6A3"),
        Arrays.asList(
            "Q6A1",
            "Q6A2"),
        Collections.singletonList(createBoolQuestion("Q6C1", 0))));

    // checklist
    questions.add(createChecklistQuestion(
        "Q7",
        order++,
        Arrays.asList(
            createChecklistEntry("Q7E1", 0),
            createChecklistEntry("Q7E2", 1),
            createChecklistEntry("Q7E3", 2))));

    // Range - no children
    questions.add(createRangeQuestion(
        "Q8",
        order++,
        1, 10,
        5,
        "Q8MIN", "Q8MAX"));

    // Range - with children
    questions.add(createRangeQuestion(
        "Q9",
        order++,
        2, 11,
        6,
        "Q9MIN", "Q9MAX",
        Collections.singletonList(createBoolQuestion("Q9C1", 0))));

    // TextField - no children
    questions.add(createTextQuestion(
        "Q10",
        order++,
        false,
        256));

    // TextField - with children
    questions.add(createTextQuestion(
        "Q11",
        order++,
        false,
        256,
        Collections.singletonList(createBoolQuestion("Q11C1", 0))));

    // TextArea - no children
    questions.add(createTextQuestion(
        "Q12",
        order++,
        true,
        512));

    // TextArea - with children
    questions.add(createTextQuestion(
        "Q13",
        order++,
        true,
        512,
        Collections.singletonList(createBoolQuestion("Q13C1", 0))));

    this.surveyRepository.save(Survey.builder()
        .questions(questions)
        .nameId("TEST")
        .title("TITLE")
        .description("DESCRIPTION")
        .intervalType(IntervalType.NONE)
        .releaseStatus(ReleaseStatusType.RELEASED)
        .build());
  }

  /**
   *
   * @param answer
   * @return
   */
  public Answer createAnswer(final String answer) {
    return this.answerRepository.save(Answer.builder()
        .value(answer)
        .build());
  }

  public Question createChoiceQuestion(
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
   * @param questions
   * @return
   */
  public Question createChoiceQuestion(
      final String question,
      final int order,
      final boolean multiple,
      final List<String> answers,
      final List<String> dependsOn,
      final List<Question> questions) {

    if (answers == null || answers.isEmpty())
      throw new IllegalArgumentException("Answers must not be null or empty.");

    final List<Answer> answerEntities = answers.stream().map(f -> createAnswer(f)).collect(Collectors.toList());

    ChoiceQuestion parent = this.questionRepository.save(ChoiceQuestion.builder()
        .question(question)
        .ranking(order)
        .answers(answerEntities)
        .multiple(multiple)
        .build());

    if (questions != null && !questions.isEmpty()) {

      final List<Answer> dependsOnAnswers = dependsOn == null || dependsOn.isEmpty() ? null
          : answerEntities.stream().filter(p -> dependsOn.contains(p.getValue())).collect(Collectors.toList());

      final ChoiceContainer container = this.containerRepository.save(ChoiceContainer.builder()
          .dependsOn(dependsOnAnswers)
          .questions(questions)
          .parent(parent)
          .build());

      parent.setContainer(container);
      parent = this.questionRepository.save(parent);
    }

    return parent;
  }

  /**
   *
   * @param question
   * @param order
   * @return
   */
  public BooleanQuestion createBoolQuestion(
      final String question,
      final int order) {

    return createBoolQuestion(question, order, null, null);
  }

  /**
   *
   * @param question
   * @param order
   * @param dependsOn
   * @param questions
   * @return
   */
  public BooleanQuestion createBoolQuestion(
      final String question,
      final int order,
      final Boolean dependsOn,
      final List<Question> questions) {

    BooleanQuestion parent = this.questionRepository.save(BooleanQuestion.builder()
        .question(question)
        .ranking(order)
        .build());

    if (questions != null && !questions.isEmpty()) {

      final BooleanContainer container = this.containerRepository.save(BooleanContainer.builder()
          .questions(questions)
          .dependsOn(dependsOn)
          .parent(parent)
          .build());

      parent.setContainer(container);
      parent = this.questionRepository.save(parent);
    }

    return parent;
  }

  public ChecklistEntry createChecklistEntry(
      final String question,
      final int order) {

    return this.questionRepository.save(ChecklistEntry.builder()
        .question(question)
        .ranking(order)
        .build());
  }

  /**
   *
   * @param question
   * @param order
   * @return
   */
  public Question createTextQuestion(
      final String question,
      final int order,
      final boolean multiline,
      final int length) {

    return createTextQuestion(question, order, multiline, length, null);
  }

  /**
   *
   * @param question
   * @param order
   * @param length
   * @param questions
   * @return
   */
  public Question createTextQuestion(
      final String question,
      final int order,
      final boolean multiline,
      final int length,
      final List<Question> questions) {

    TextQuestion parent = this.questionRepository.save(TextQuestion.builder()
        .question(question)
        .multiline(multiline)
        .ranking(order)
        .length(length)
        .build());

    if (questions != null && !questions.isEmpty()) {

      final DefaultContainer container = this.containerRepository.save(DefaultContainer.builder()
          .questions(questions)
          .parent(parent)
          .build());

      parent.setContainer(container);
      parent = this.questionRepository.save(parent);
    }

    return parent;
  }

  /**
   *
   * @param question
   * @param order
   * @param entries
   * @return
   */
  public Question createChecklistQuestion(
      final String question,
      final int order,
      final List<ChecklistEntry> entries) {

    if (entries == null || entries.isEmpty())
      throw new IllegalArgumentException("Entries must not be null or empty.");

    return this.questionRepository.save(ChecklistQuestion.builder()
        .question(question)
        .entries(entries)
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
  public Question createRangeQuestion(
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
   * @param questions
   * @return
   */
  public Question createRangeQuestion(
      final String question,
      final int order,
      final int minValue, final int maxValue,
      final Integer defaultValue,
      final String minText, final String maxText,
      final List<Question> questions) {

    RangeQuestion parent = this.questionRepository.save(RangeQuestion.builder()
        .question(question)
        .minValue(minValue)
        .maxValue(maxValue)
        .defaultAnswer(defaultValue)
        .minText(minText)
        .maxText(maxText)
        .ranking(order)
        .build());

    if (questions != null && !questions.isEmpty()) {

      final DefaultContainer container = this.containerRepository.save(DefaultContainer.builder()
          .questions(questions)
          .parent(parent)
          .build());

      parent.setContainer(container);
      parent = this.questionRepository.save(parent);
    }

    return parent;
  }
}
