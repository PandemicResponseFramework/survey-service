/**
 *
 */
package one.tracking.framework.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import one.tracking.framework.domain.SearchResult;
import one.tracking.framework.dto.meta.question.BooleanQuestionDto;
import one.tracking.framework.dto.meta.question.ChecklistEntryDto;
import one.tracking.framework.dto.meta.question.ChoiceQuestionDto;
import one.tracking.framework.dto.meta.question.QuestionDto;
import one.tracking.framework.dto.meta.question.QuestionType;
import one.tracking.framework.dto.meta.question.RangeQuestionDto;
import one.tracking.framework.dto.meta.question.TextQuestionDto;
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
import one.tracking.framework.entity.meta.question.IContainerQuestion;
import one.tracking.framework.entity.meta.question.Question;
import one.tracking.framework.entity.meta.question.RangeQuestion;
import one.tracking.framework.entity.meta.question.TextQuestion;
import one.tracking.framework.exception.ConflictException;
import one.tracking.framework.repo.AnswerRepository;
import one.tracking.framework.repo.ContainerRepository;
import one.tracking.framework.repo.QuestionRepository;
import one.tracking.framework.repo.SurveyRepository;

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

  /**
   *
   * @param nameId
   * @return
   */
  public Survey createNewSurveyVersion(final String nameId) {

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
   *
   * @param nameId
   * @param data
   * @return
   */
  public Question updateQuestion(final String nameId, final QuestionDto data) {

    final List<Survey> surveys = this.surveyRepository.findByNameIdOrderByVersionDesc(nameId);

    if (surveys == null || surveys.isEmpty())
      throw new IllegalArgumentException(
          "No survey found for nameId: " + nameId + " and questionId: " + data.getId());

    final Survey survey = surveys.get(0);

    if (survey.getReleaseStatus() == ReleaseStatusType.RELEASED)
      throw new ConflictException("Current survey with nameId: " + nameId + " got released already.");

    /*
     * We have to check, if the specified ID in questionData is actually part of the current survey
     * entity. So we have to look for it and because of that, we do not need to request the question
     * entity via the question repository.
     */
    final SearchResult searchResult = searchQuestion(survey.getQuestions(), data.getId());

    if (searchResult == null)
      throw new IllegalArgumentException(
          "Specified questionId is not part of the current survey: questionId: " + data.getId());

    final Question question = searchResult.getQuestion();
    final int currentRanking = question.getRanking();

    final QuestionType dataType = QuestionType.valueOf(data.getClass());

    if (!question.getType().equals(dataType))
      throw new IllegalArgumentException("The question type does not match the expected question type. Expected: "
          + question.getType() + "; Received: " + dataType);

    // FIXME start ranking/order at zero in example data and IT
    if (data.getOrder() >= searchResult.getQuestionContainer().size())
      throw new IllegalArgumentException("The specified order is greater than the possible value. Expected: "
          + searchResult.getQuestionContainer().size() + " Received: " + data.getOrder());

    updateQuestionData(question, data);

    // Persist updates
    final Question updatedQuestion = this.questionRepository.save(question);

    // Update ranking of siblings if required
    if (question.getRanking() != currentRanking)
      updateRankings(searchResult, updatedQuestion);

    return updatedQuestion;
  }

  /**
   * @param searchResult
   * @param updatedQuestion
   */
  private void updateRankings(final SearchResult searchResult, final Question updatedQuestion) {

    for (int i = 0; i < searchResult.getQuestionContainer().size(); i++) {

      final Question currentSibling = searchResult.getQuestionContainer().get(i);

      // Skip updating updatedQuestion
      if (currentSibling.getId().equals(updatedQuestion.getId()))
        continue;

      if (currentSibling.getRanking() <= updatedQuestion.getRanking()) {

        currentSibling.setRanking(i);
        this.questionRepository.save(currentSibling);

      } else {
        break;
      }
    }
  }

  private SearchResult searchQuestion(final List<Question> questions, final Long id) {

    if (questions == null || questions.isEmpty())
      return null;

    for (int i = 0; i < questions.size(); i++) {

      final Question question = questions.get(i);

      if (question.hasContainer()) {
        final SearchResult result =
            searchQuestion(((IContainerQuestion) question).getContainer().getQuestions(), id);
        if (result != null)
          return result;
      }

      if (question.getId().equals(id))
        return SearchResult.builder()
            .question(question)
            .questionContainer(questions)
            .position(i)
            .build();
    }

    return null;
  }

  private void updateQuestionData(final Question question, final QuestionDto data) {

    question.setQuestion(data.getQuestion());
    question.setRanking(data.getOrder());

    switch (question.getType()) {
      case BOOL:
        updateQuestion((BooleanQuestion) question, (BooleanQuestionDto) data);
        break;
      case CHECKLIST:
        // nothing special
        break;
      case CHECKLIST_ENTRY:
        updateQuestion((ChecklistEntry) question, (ChecklistEntryDto) data);
        break;
      case CHOICE:
        updateQuestion((ChoiceQuestion) question, (ChoiceQuestionDto) data);
        break;
      case RANGE:
        updateQuestion((RangeQuestion) question, (RangeQuestionDto) data);
        break;
      case TEXT:
        updateQuestion((TextQuestion) question, (TextQuestionDto) data);
        break;
      default:
        break;
    }
  }

  private void updateQuestion(final BooleanQuestion question, final BooleanQuestionDto data) {

    question.setDefaultAnswer(data.getDefaultAnswer());
  }

  private void updateQuestion(final ChecklistEntry question, final ChecklistEntryDto data) {

    question.setDefaultAnswer(data.getDefaultAnswer());
  }

  private void updateQuestion(final ChoiceQuestion question, final ChoiceQuestionDto data) {

    final Optional<Answer> answerOp = question.getAnswers().stream()
        .filter(p -> p.getId().equals(data.getDefaultAnswer()))
        .reduce((a, b) -> {
          throw new IllegalStateException("Multiple elements: " + a + ", " + b);
        });

    if (answerOp.isEmpty())
      throw new IllegalArgumentException(
          "Specified default answer ID does not exists in the question scope. Specified: " + data.getDefaultAnswer());

    question.setDefaultAnswer(answerOp.get());
    question.setMultiple(data.isMultiple());
  }

  private void updateQuestion(final RangeQuestion question, final RangeQuestionDto data) {

    question.setDefaultAnswer(data.getDefaultValue());
    question.setMaxText(data.getMaxText());
    question.setMaxValue(data.getMaxValue());
    question.setMinText(data.getMinText());
    question.setMinValue(data.getMinValue());
  }

  private void updateQuestion(final TextQuestion question, final TextQuestionDto data) {

    question.setLength(data.getLength());
    question.setMultiline(data.isMultiline());
  }

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

    final List<Question> questions = copyQuestions(container.getQuestions());

    return this.containerRepository.save(container.toBuilder()
        .id(null)
        .createdAt(null)
        .questions(questions)
        .build());
  }

  private ChoiceContainer copyContainer(final ChoiceContainer container, final List<Answer> answersCopy) {

    if (container == null)
      return null;

    final List<Question> questions = copyQuestions(container.getQuestions());

    List<Answer> dependsOn = null;

    if (container.getDependsOn() != null && !container.getDependsOn().isEmpty()) {

      dependsOn = answersCopy.stream()
          .filter(p -> container.getDependsOn().stream().anyMatch(m -> m.getValue().equals(p.getValue())))
          .collect(Collectors.toList());
    }

    return this.containerRepository.save(container.toBuilder()
        .id(null)
        .createdAt(null)
        .questions(questions)
        .dependsOn(dependsOn)
        .build());
  }

  private DefaultContainer copyContainer(final DefaultContainer container) {

    if (container == null)
      return null;

    final List<Question> questions = copyQuestions(container.getQuestions());

    return this.containerRepository.save(container.toBuilder()
        .id(null)
        .createdAt(null)
        .questions(questions)
        .build());
  }

}
