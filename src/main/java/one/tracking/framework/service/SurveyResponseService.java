/**
 *
 */
package one.tracking.framework.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import one.tracking.framework.dto.SurveyResponseDto;
import one.tracking.framework.entity.SurveyInstance;
import one.tracking.framework.entity.SurveyResponse;
import one.tracking.framework.entity.SurveyStatus;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.container.Container;
import one.tracking.framework.entity.meta.question.BooleanQuestion;
import one.tracking.framework.entity.meta.question.ChecklistEntry;
import one.tracking.framework.entity.meta.question.ChecklistQuestion;
import one.tracking.framework.entity.meta.question.ChoiceQuestion;
import one.tracking.framework.entity.meta.question.IContainerQuestion;
import one.tracking.framework.entity.meta.question.Question;
import one.tracking.framework.entity.meta.question.RangeQuestion;
import one.tracking.framework.entity.meta.question.TextQuestion;
import one.tracking.framework.repo.AnswerRepository;
import one.tracking.framework.repo.ContainerRepository;
import one.tracking.framework.repo.QuestionRepository;
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

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private SurveyRepository surveyRepository;

  @Autowired
  private SurveyInstanceRepository surveyInstanceRepository;

  @Autowired
  private QuestionRepository questionRepository;

  @Autowired
  private SurveyStatusRepository surveyStatusRepository;

  @Autowired
  private SurveyResponseRepository surveyResponseRepository;

  @Autowired
  private AnswerRepository answerRepository;

  @Autowired
  private ContainerRepository containerRepository;

  @Transactional
  public void handleSurveyResponse(final String userId, final String nameId, final SurveyResponseDto surveyResponse) {

    final User user = this.userRepository.findById(userId).get();
    final Survey survey = this.surveyRepository.findTopByNameIdOrderByVersionDesc(nameId).get();
    final SurveyInstance instance = this.surveyInstanceRepository.findBySurveyAndToken(
        survey, surveyResponse.getSurveyToken()).get();
    final Question question = this.questionRepository.findById(surveyResponse.getQuestionId()).get();

    if (!validateResponse(question, surveyResponse))
      throw new IllegalArgumentException("Invalid Survey Response.");

    Question nextQuestion = null;

    switch (question.getType()) {
      case BOOL:
        storeBooleanResponse(surveyResponse, user, instance, question);
        nextQuestion = getNextSubQuestion((BooleanQuestion) question, surveyResponse);
        break;
      case CHECKLIST:
        storeChecklistResponse(surveyResponse, user, instance, question);
        break;
      case CHOICE:
        storeChoiceResponse(surveyResponse, user, instance, question);
        nextQuestion = getNextSubQuestion((ChoiceQuestion) question, surveyResponse);
        break;
      case RANGE:
        storeRangeResponse(surveyResponse, user, instance, question);
        break;
      case TEXT:
        storeTextResponse(surveyResponse, user, instance, question);
        break;
      default:
        // nothing got changed -> no need to continue
        return;
    }

    if (nextQuestion == null)
      nextQuestion = seekNextQuestion(question);

    deleteSubQuestionTree(user, instance, question);

    final Optional<SurveyStatus> statusOp = this.surveyStatusRepository.findByUserAndSurveyInstance(user, instance);

    if (statusOp.isEmpty()) {

      this.surveyStatusRepository.save(SurveyStatus.builder()
          .lastQuestion(question)
          .nextQuestion(nextQuestion)
          .surveyInstance(instance)
          .user(user)
          .build());
    } else {

      final SurveyStatus status = statusOp.get();
      status.setLastQuestion(question);
      status.setNextQuestion(nextQuestion);
      this.surveyStatusRepository.save(status);
    }
  }

  private Question seekNextQuestion(final Question question) {

    if (question == null)
      return null;

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
      }
    }

    // If no next sibling exists go up a level and look for next sibling of parent question
    if (result == null)
      return seekNextQuestion(container.getParent());
    else
      return result;

  }

  private Question getNextSubQuestion(final BooleanQuestion question, final SurveyResponseDto response) {

    if (!question.hasContainer()
        || question.getContainer().getQuestions() == null
        || question.getContainer().getQuestions().isEmpty()
        || !response.getBoolAnswer().equals(question.getContainer().getDependsOn()))
      return null;

    return question.getContainer().getQuestions().get(0);
  }

  private Question getNextSubQuestion(final ChoiceQuestion question, final SurveyResponseDto response) {

    if (!question.hasContainer()
        || question.getContainer().getQuestions() == null
        || question.getContainer().getQuestions().isEmpty()
        || question.getContainer().getDependsOn() == null
        || question.getContainer().getDependsOn().isEmpty()
        || question.getContainer().getDependsOn().stream().noneMatch(p -> response.getAnswerIds().contains(p.getId())))
      return null;

    return question.getContainer().getQuestions().get(0);
  }

  /**
   * @param surveyResponse
   * @param user
   * @param instance
   * @param question
   */
  private final void storeChecklistResponse(
      final SurveyResponseDto surveyResponse,
      final User user,
      final SurveyInstance instance,
      final Question question) {

    final ChecklistQuestion checklistQuestion = (ChecklistQuestion) question;
    for (final ChecklistEntry entry : checklistQuestion.getEntries()) {

      final Optional<SurveyResponse> entityOp =
          this.surveyResponseRepository.findByUserAndSurveyInstanceAndQuestion(user, instance, entry);
      final Boolean response = surveyResponse.getChecklistAnswer().get(entry.getId());

      if (entityOp.isEmpty()) {

        this.surveyResponseRepository.save(SurveyResponse.builder()
            .question(entry)
            .surveyInstance(instance)
            .user(user)
            .boolAnswer(response == null ? false : response)
            .build());

      } else {

        final SurveyResponse entity = entityOp.get();
        entity.setBoolAnswer(response == null ? false : response);
        this.surveyResponseRepository.save(entity);
      }
    }
  }

  private final void storeBooleanResponse(
      final SurveyResponseDto surveyResponse,
      final User user,
      final SurveyInstance instance,
      final Question question) {

    final Optional<SurveyResponse> entityOp =
        this.surveyResponseRepository.findByUserAndSurveyInstanceAndQuestion(user, instance, question);

    if (entityOp.isEmpty()) {

      this.surveyResponseRepository.save(SurveyResponse.builder()
          .question(question)
          .surveyInstance(instance)
          .user(user)
          .boolAnswer(surveyResponse.getBoolAnswer())
          .build());

    } else {

      final SurveyResponse entity = entityOp.get();
      entity.setBoolAnswer(surveyResponse.getBoolAnswer());
      this.surveyResponseRepository.save(entity);
    }
  }

  private final void storeChoiceResponse(
      final SurveyResponseDto surveyResponse,
      final User user,
      final SurveyInstance instance,
      final Question question) {

    final List<Answer> existingAnswers = new ArrayList<>(surveyResponse.getAnswerIds().size());

    for (final Long answerId : surveyResponse.getAnswerIds()) {

      final Optional<Answer> answerOp = this.answerRepository.findById(answerId);

      if (answerOp.isEmpty())
        throw new IllegalStateException("Unexpected state: Could not find answer entity for id: " + answerId);

      existingAnswers.add(answerOp.get());
    }

    final Optional<SurveyResponse> entityOp =
        this.surveyResponseRepository.findByUserAndSurveyInstanceAndQuestion(user, instance, question);

    if (entityOp.isEmpty()) {

      this.surveyResponseRepository.save(SurveyResponse.builder()
          .question(question)
          .surveyInstance(instance)
          .user(user)
          .answers(existingAnswers)
          .build());

    } else {

      final SurveyResponse entity = entityOp.get();
      entity.setAnswers(existingAnswers);
      this.surveyResponseRepository.save(entity);
    }
  }

  private final void storeRangeResponse(
      final SurveyResponseDto surveyResponse,
      final User user,
      final SurveyInstance instance,
      final Question question) {

    final Optional<SurveyResponse> entityOp =
        this.surveyResponseRepository.findByUserAndSurveyInstanceAndQuestion(user, instance, question);

    if (entityOp.isEmpty()) {

      this.surveyResponseRepository.save(SurveyResponse.builder()
          .question(question)
          .surveyInstance(instance)
          .user(user)
          .rangeAnswer(surveyResponse.getRangeAnswer())
          .build());

    } else {

      final SurveyResponse entity = entityOp.get();
      entity.setRangeAnswer(surveyResponse.getRangeAnswer());
      this.surveyResponseRepository.save(entity);
    }
  }

  private final void storeTextResponse(
      final SurveyResponseDto surveyResponse,
      final User user,
      final SurveyInstance instance,
      final Question question) {

    final Optional<SurveyResponse> entityOp =
        this.surveyResponseRepository.findByUserAndSurveyInstanceAndQuestion(user, instance, question);

    if (entityOp.isEmpty()) {

      this.surveyResponseRepository.save(SurveyResponse.builder()
          .question(question)
          .surveyInstance(instance)
          .user(user)
          .textAnswer(surveyResponse.getTextAnswer())
          .build());

    } else {

      final SurveyResponse entity = entityOp.get();
      entity.setTextAnswer(surveyResponse.getTextAnswer());
      this.surveyResponseRepository.save(entity);
    }
  }

  private final void deleteSubQuestionTree(
      final User user,
      final SurveyInstance instance,
      final Question question) {

    if (!(question instanceof IContainerQuestion))
      return;

    final IContainerQuestion cQuestion = (IContainerQuestion) question;

    if (cQuestion.getContainer() == null
        || cQuestion.getContainer().getQuestions() == null
        || cQuestion.getContainer().getQuestions().isEmpty())
      return;

    for (final Question subQuestion : cQuestion.getContainer().getQuestions()) {

      deleteSubQuestionTree(user, instance, subQuestion);

      this.surveyResponseRepository.deleteByUserAndSurveyInstanceAndQuestion(user, instance, subQuestion);
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

    if (response.getChecklistAnswer() == null)
      return false;

    final ChecklistQuestion checklistQuestion = (ChecklistQuestion) question;
    final List<Long> originQuestionIds =
        checklistQuestion.getEntries().stream().map(Question::getId).collect(Collectors.toList());

    // Does modify the response object but the request will be denied if modification occurred
    return !response.getChecklistAnswer().keySet().retainAll(originQuestionIds);
  }

  private boolean validateResponse(final Question question, final SurveyResponseDto response) {

    switch (question.getType()) {
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
      case CHECKLIST_ENTRY:
      default:
        return false;
    }
  }
}
