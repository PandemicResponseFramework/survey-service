/**
 *
 */
package one.tracking.framework.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import one.tracking.framework.entity.meta.Answer;
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
import one.tracking.framework.service.exception.ConflictException;

/**
 * @author Marko Voß
 *
 */
@Service
public class SurveyManagementService {

  @Autowired
  private SurveyRepository surveyRepository;

  @Autowired
  private ContainerRepository containerRepository;

  @Autowired
  private QuestionRepository questionRepository;

  @Autowired
  private AnswerRepository answerRepository;

  public Survey createNewVersion(final String nameId) {

    final List<Survey> surveys = this.surveyRepository.findByNameIdOrderByVersionDesc(nameId);

    if (surveys == null || surveys.isEmpty())
      throw new IllegalArgumentException("No survey found for nameId: " + nameId);

    if (surveys.get(0).getReleaseStatus() != ReleaseStatusType.RELEASED)
      throw new ConflictException("Current survey with nameId: " + nameId + " is not released.");

    final Survey currentRelease = surveys.get(0);

    final List<Question> copiedQuestions = copyQuestions(currentRelease.getQuestions());

    return this.surveyRepository.save(currentRelease.toBuilder()
        .id(null)
        .createdAt(null)
        .version(currentRelease.getVersion() + 1)
        .releaseStatus(ReleaseStatusType.NEW)
        .questions(copiedQuestions)
        .build());
  }

  /**
   * @param questions
   * @return
   */
  private List<Question> copyQuestions(final List<Question> questions) {

    if (questions == null)
      return null;

    final List<Question> copies = new ArrayList<>(questions.size());

    for (final Question question : questions) {

      switch (question.getType()) {
        case BOOL:
          copies.add(copyQuestion((BooleanQuestion) question));
          break;
        case CHECKLIST:
          copies.add(copyQuestion((ChecklistQuestion) question));
          break;
        case CHOICE:
          copies.add(copyQuestion((ChoiceQuestion) question));
          break;
        case RANGE:
          copies.add(copyQuestion((RangeQuestion) question));
          break;
        case TEXT:
          copies.add(copyQuestion((TextQuestion) question));
          break;
        default:
          break;

      }
    }

    return copies;
  }

  private ChecklistQuestion copyQuestion(final ChecklistQuestion question) {

    final List<ChecklistEntry> entries = new ArrayList<>();

    for (final ChecklistEntry entry : question.getEntries()) {
      entries.add(copyQuestion(entry));
    }

    return this.questionRepository.save(question.toBuilder()
        .id(null)
        .createdAt(null)
        .entries(entries)
        .build());
  }

  private ChecklistEntry copyQuestion(final ChecklistEntry entry) {

    return this.questionRepository.save(entry.toBuilder()
        .id(null)
        .createdAt(null)
        .build());
  }

  private BooleanQuestion copyQuestion(final BooleanQuestion question) {

    final BooleanContainer container = copyContainer(question.getContainer());

    return this.questionRepository.save(question.toBuilder()
        .id(null)
        .createdAt(null)
        .container(container)
        .build());
  }

  private Question copyQuestion(final RangeQuestion question) {

    final DefaultContainer container = copyContainer(question.getContainer());

    return this.questionRepository.save(question.toBuilder()
        .id(null)
        .createdAt(null)
        .container(container)
        .build());
  }

  private TextQuestion copyQuestion(final TextQuestion question) {

    final DefaultContainer container = copyContainer(question.getContainer());

    return this.questionRepository.save(question.toBuilder()
        .id(null)
        .createdAt(null)
        .container(container)
        .build());
  }

  private ChoiceQuestion copyQuestion(final ChoiceQuestion question) {

    final List<Answer> answers = copyAnswers(question.getAnswers());

    final ChoiceContainer container = copyContainer(question.getContainer(), answers);

    return this.questionRepository.save(question.toBuilder()
        .id(null)
        .createdAt(null)
        .answers(answers)
        .container(container)
        .build());
  }

  /**
   * @param answers
   * @return
   */
  private List<Answer> copyAnswers(final List<Answer> answers) {

    if (answers == null || answers.isEmpty())
      return null;

    final List<Answer> copies = new ArrayList<>(answers.size());

    for (final Answer answer : answers) {

      copies.add(this.answerRepository.save(answer.toBuilder()
          .id(null)
          .createdAt(null)
          .build()));
    }

    return copies;
  }

  private BooleanContainer copyContainer(final BooleanContainer container) {

    if (container == null)
      return null;

    final List<Question> questions = copyQuestions(container.getSubQuestions());

    return this.containerRepository.save(container.toBuilder()
        .id(null)
        .createdAt(null)
        .subQuestions(questions)
        .build());
  }

  private ChoiceContainer copyContainer(final ChoiceContainer container, final List<Answer> answersCopy) {

    if (container == null)
      return null;

    final List<Question> questions = copyQuestions(container.getSubQuestions());

    List<Answer> dependsOn = null;

    if (container.getDependsOn() != null && !container.getDependsOn().isEmpty()) {

      dependsOn = answersCopy.stream()
          .filter(p -> container.getDependsOn().stream().anyMatch(m -> m.getValue().equals(p.getValue())))
          .collect(Collectors.toList());
    }

    return this.containerRepository.save(container.toBuilder()
        .id(null)
        .createdAt(null)
        .subQuestions(questions)
        .dependsOn(dependsOn)
        .build());
  }

  private DefaultContainer copyContainer(final DefaultContainer container) {

    if (container == null)
      return null;

    final List<Question> questions = copyQuestions(container.getSubQuestions());

    return this.containerRepository.save(container.toBuilder()
        .id(null)
        .createdAt(null)
        .subQuestions(questions)
        .build());
  }

}
