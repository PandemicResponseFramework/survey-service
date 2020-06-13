/**
 *
 */
package one.tracking.framework.service;

import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import one.tracking.framework.component.SurveyResponseComponent;
import one.tracking.framework.domain.Period;
import one.tracking.framework.domain.SurveyStatusChange;
import one.tracking.framework.domain.SurveyStatusType;
import one.tracking.framework.dto.SurveyResponseConflictType;
import one.tracking.framework.dto.SurveyResponseDto;
import one.tracking.framework.entity.SurveyInstance;
import one.tracking.framework.entity.SurveyResponse;
import one.tracking.framework.entity.SurveyStatus;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.ReleaseStatusType;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.container.Container;
import one.tracking.framework.entity.meta.question.ChecklistQuestion;
import one.tracking.framework.entity.meta.question.ChoiceQuestion;
import one.tracking.framework.entity.meta.question.NumberQuestion;
import one.tracking.framework.entity.meta.question.Question;
import one.tracking.framework.entity.meta.question.RangeQuestion;
import one.tracking.framework.entity.meta.question.TextQuestion;
import one.tracking.framework.exception.SurveyResponseConflictException;
import one.tracking.framework.repo.ContainerRepository;
import one.tracking.framework.repo.SurveyInstanceRepository;
import one.tracking.framework.repo.SurveyRepository;
import one.tracking.framework.repo.SurveyResponseRepository;
import one.tracking.framework.repo.SurveyStatusRepository;
import one.tracking.framework.repo.UserRepository;

/**
 * @author Marko Voß
 *
 */
@Service
public class SurveyResponseService {

  private static final Logger LOG = LoggerFactory.getLogger(SurveyResponseService.class);

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private SurveyRepository surveyRepository;

  @Autowired
  private SurveyInstanceRepository surveyInstanceRepository;

  @Autowired
  private SurveyStatusRepository surveyStatusRepository;

  @Autowired
  private ContainerRepository containerRepository;

  @Autowired
  private SurveyResponseComponent surveyResponseComponent;

  @Autowired
  private SurveyResponseRepository surveyResponseRepository;

  @Autowired
  private ServiceUtility utility;

  @Transactional
  public void handleSurveyResponse(final String userId, final String nameId, final SurveyResponseDto surveyResponse)
      throws SurveyResponseConflictException {

    final User user = this.userRepository.findById(userId).get();

    final Survey survey = this.surveyRepository
        .findTopByNameIdAndReleaseStatusOrderByVersionDesc(nameId, ReleaseStatusType.RELEASED).get();

    final Optional<SurveyInstance> instanceOp = this.surveyInstanceRepository.findBySurveyAndToken(
        survey, surveyResponse.getSurveyToken());

    if (instanceOp.isEmpty())
      throw new SurveyResponseConflictException(SurveyResponseConflictType.INVALID_SURVEY_TOKEN);

    final SurveyInstance instance = instanceOp.get();

    if (Instant.now().isAfter(instance.getEndTime()))
      throw new SurveyResponseConflictException(SurveyResponseConflictType.INVALID_SURVEY_TOKEN);

    if (!checkIfDependencyIsSatisfied(user, survey))
      throw new SurveyResponseConflictException(SurveyResponseConflictType.UNSATISFIED_DEPENDENCY);

    final Question question = getQuestion(survey.getQuestions(), surveyResponse.getQuestionId());

    if (question == null)
      throw new IllegalArgumentException("Provided questionId is not part of the current survey.");

    if (!validateResponse(question, surveyResponse))
      throw new IllegalArgumentException("Invalid survey response.");

    final SurveyStatusChange statusChange =
        this.surveyResponseComponent.persistSurveyResponse(user, instance, question, surveyResponse);

    if (statusChange.isSkipUpdate())
      return;

    final Question nextQuestion = statusChange.hasNextQuestion()
        ? statusChange.getNextQuestion()
        : seekNextQuestion(question);

    final Optional<SurveyStatus> statusOp = this.surveyStatusRepository.findByUserAndSurveyInstance(user, instance);

    if (statusOp.isEmpty()) {

      this.surveyStatusRepository.save(SurveyStatus.builder()
          .nextQuestion(nextQuestion)
          .surveyInstance(instance)
          .user(user)
          .build());
    } else {

      final SurveyStatus status = statusOp.get();
      status.setNextQuestion(nextQuestion);
      this.surveyStatusRepository.save(status);
    }
  }

  private Question getQuestion(final List<Question> questions, final Long questionId) {

    if (questions == null || questions.isEmpty() || questionId == null)
      return null;

    for (final Question question : questions) {

      if (questionId.equals(question.getId()))
        return question;

      final List<Question> subQuestions = question.getSubQuestions();
      if (subQuestions == null || subQuestions.isEmpty())
        continue;

      final Question result = getQuestion(subQuestions, questionId);
      if (result != null)
        return result;
    }

    return null;
  }

  private boolean checkIfDependencyIsSatisfied(final User user, final Survey survey) {

    if (survey.getDependsOn() == null)
      return true;

    final Period period = this.utility.getCurrentSurveyInstancePeriod(survey.getDependsOn());

    final Optional<SurveyInstance> dependsOnInstanceOp = this.surveyInstanceRepository
        .findBySurveyAndStartTimeAndEndTime(survey.getDependsOn(), period.getStart(), period.getEnd());

    if (dependsOnInstanceOp.isEmpty())
      return false;

    final List<SurveyResponse> surveyResponses =
        this.surveyResponseRepository.findByUserAndSurveyInstanceAndMaxVersion(user, dependsOnInstanceOp.get());

    return this.utility.calculateSurveyStatus(survey.getDependsOn(), surveyResponses) == SurveyStatusType.COMPLETED;
  }

  private Question seekNextQuestion(final Question question) {

    if (question == null)
      return null;

    LOG.debug("Seeking next question. Current: {}", question.getQuestion());

    final Optional<Container> containerOp =
        this.containerRepository.findByQuestionsIn(Collections.singleton(question));

    if (containerOp.isEmpty())
      return null;

    final Container container = containerOp.get();

    Question result = null;

    final Iterator<Question> it = container.getQuestions().iterator();

    while (it.hasNext()) {
      // Find current question and return next sibling if exists
      if (it.next().getId().equals(question.getId())) {
        result = it.hasNext() ? it.next() : null;
        break;
      }
    }

    // If no next sibling exists go up a level and look for next sibling of parent question
    if (result == null)
      return seekNextQuestion(container.getParent());
    else
      return result;

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
    return response.getNumberAnswer() != null
        && response.getNumberAnswer() >= rangeQuestion.getMinValue()
        && response.getNumberAnswer() <= rangeQuestion.getMaxValue();
  }

  private final boolean validateNumberResponse(final Question question, final SurveyResponseDto response) {

    final NumberQuestion numberQuestion = (NumberQuestion) question;
    return response.getNumberAnswer() != null
        && (numberQuestion.getMinValue() == null || response.getNumberAnswer() >= numberQuestion.getMinValue())
        && (numberQuestion.getMaxValue() == null || response.getNumberAnswer() <= numberQuestion.getMaxValue());
  }

  private boolean validateChecklistResponse(final Question question, final SurveyResponseDto response) {

    if (response.getChecklistAnswer() == null)
      return false;

    final ChecklistQuestion checklistQuestion = (ChecklistQuestion) question;
    final List<Long> originQuestionIds =
        checklistQuestion.getEntries().stream().map(Question::getId).collect(Collectors.toList());

    // Does modify the response object but the request will be denied if modification occurred
    return !response.getChecklistAnswer().keySet().retainAll(originQuestionIds);
  }

  private boolean validateResponse(final Question question, final SurveyResponseDto response) {

    /*
     * Skipped overwrites everything. If skipped is set to true, everything else can be ignored
     */
    if (question.isOptional() && Boolean.TRUE.equals(response.getSkipped()))
      return true;

    switch (question.getType()) {
      case BOOL:
        return validateBoolResponse(question, response);
      case CHOICE:
        return validateChoiceResponse(question, response);
      case RANGE:
        return validateRangeResponse(question, response);
      case TEXT:
        return validateTextResponse(question, response);
      case NUMBER:
        return validateNumberResponse(question, response);
      case CHECKLIST:
        return validateChecklistResponse(question, response);
      case CHECKLIST_ENTRY:
      default:
        return false;
    }
  }
}
