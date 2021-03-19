/**
 *
 */
package one.tracking.framework.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import one.tracking.framework.domain.SearchResult;
import one.tracking.framework.domain.SurveyStatusChange;
import one.tracking.framework.dto.SurveyResponseDto;
import one.tracking.framework.entity.SurveyInstance;
import one.tracking.framework.entity.SurveyResponse;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.ReleaseStatusType;
import one.tracking.framework.entity.meta.container.Container;
import one.tracking.framework.entity.meta.question.BooleanQuestion;
import one.tracking.framework.entity.meta.question.ChecklistEntry;
import one.tracking.framework.entity.meta.question.ChecklistQuestion;
import one.tracking.framework.entity.meta.question.ChoiceQuestion;
import one.tracking.framework.entity.meta.question.Question;
import one.tracking.framework.repo.AnswerRepository;
import one.tracking.framework.repo.SurveyResponseRepository;
import one.tracking.framework.service.TemporaryHelperService;

/**
 * @author Marko Voß
 *
 */
@Component
@Transactional
public class SurveyResponseComponent {

  private static final Logger LOG = LoggerFactory.getLogger(SurveyResponseComponent.class);

  @Autowired
  private SurveyResponseRepository surveyResponseRepository;

  @Autowired
  private AnswerRepository answerRepository;

  @Autowired
  private TemporaryHelperService helperService;

  public SurveyStatusChange persistSurveyResponse(final User user, final SurveyInstance instance,
      final Question question,
      final SurveyResponseDto surveyResponse) {

    LOG.debug("Persisting survey response for user '{}'; Question: '{}', Response: {}", user.getId(),
        question.getQuestion(), surveyResponse);

    if (!checkIfParentQuestionIsValid(user, instance, question))
      return SurveyStatusChange.skip();

    invalidateSubQuestionTree(user, instance, question);
    invalidateSuccessiveQuestions(user, instance, question);

    switch (question.getType()) {
      case BOOL:
        storeBooleanResponse(surveyResponse, user, instance, question);
        return SurveyStatusChange.withNextQuestion(getNextSubQuestion((BooleanQuestion) question, surveyResponse));
      case CHECKLIST:
        storeChecklistResponse(surveyResponse, user, instance, question);
        break;
      case CHOICE:
        storeChoiceResponse(surveyResponse, user, instance, question);
        return SurveyStatusChange.withNextQuestion(getNextSubQuestion((ChoiceQuestion) question, surveyResponse));
      case RANGE:
      case NUMBER:
        storeNumberResponse(surveyResponse, user, instance, question);
        break;
      case TEXT:
        storeTextResponse(surveyResponse, user, instance, question);
        break;
      default:
    }
    return SurveyStatusChange.noSkip();
  }

  /**
   * @return
   */
  private boolean checkIfParentQuestionIsValid(final User user, final SurveyInstance instance,
      final Question question) {

    final List<SearchResult> results = this.helperService.searchSurveys(question);

    final Optional<SearchResult> resultOp = results.stream()
        .filter(result -> result.getSurvey().getReleaseStatus() == ReleaseStatusType.RELEASED)
        .reduce((a, b) -> {
          throw new IllegalStateException("Multiple elements: " + a + ", " + b);
        });

    if (resultOp.isEmpty())
      return false;

    final Container container = resultOp.get().getOriginContainer();
    final Question parent = container.getParent();

    if (parent == null)
      return true;

    final Optional<SurveyResponse> surveyResponseOp = this.surveyResponseRepository
        .findTopByUserAndSurveyInstanceAndQuestionOrderByVersionDesc(user, instance, parent);

    return !surveyResponseOp.isEmpty() && !surveyResponseOp.get().isSkipped() && surveyResponseOp.get().isValid();
  }

  private final void invalidateSuccessiveQuestions(
      final User user,
      final SurveyInstance instance,
      final Question currentQuestion) {

    /*
     * Invalidate next siblings
     */

    final List<SearchResult> results = this.helperService.searchSurveys(currentQuestion);

    final Optional<SearchResult> resultOp = results.stream()
        .filter(result -> result.getSurvey().getReleaseStatus() == ReleaseStatusType.RELEASED)
        .reduce((a, b) -> {
          throw new IllegalStateException("Multiple elements: " + a + ", " + b);
        });

    if (resultOp.isEmpty()) {
      LOG.warn("Current question is not part of a container! QuestionId: {}", currentQuestion.getId());
      return; // should not occur
    }

    final Container container = resultOp.get().getOriginContainer();

    boolean found = false;
    for (final Question question : container.getQuestions()) {

      if (found && invalidateSurveyResponse(user, instance, question))
        invalidateSubQuestionTree(user, instance, question);

      if (!found && question.getId().equals(currentQuestion.getId()))
        found = true;
    }

    /*
     * Invalidate next parent siblings
     */
    if (container.getParent() == null)
      return;

    invalidateSuccessiveQuestions(user, instance, container.getParent());
  }

  private final void invalidateSubQuestionTree(
      final User user,
      final SurveyInstance instance,
      final Question question) {

    if (question.getSubQuestions() == null || question.getSubQuestions().isEmpty())
      return;

    for (final Question subQuestion : question.getSubQuestions()) {

      if (invalidateSurveyResponse(user, instance, subQuestion))
        invalidateSubQuestionTree(user, instance, subQuestion);
    }
  }

  private final boolean invalidateSurveyResponse(final User user, final SurveyInstance instance,
      final Question question) {

    final Optional<SurveyResponse> responseOp = this.surveyResponseRepository
        .findTopByUserAndSurveyInstanceAndQuestionAndValidOrderByVersionDesc(user, instance, question, true);

    if (responseOp.isPresent()) {

      this.surveyResponseRepository.save(responseOp.get().toBuilder()
          .valid(false)
          .build());
      return true;
    }
    return false;
  }

  private final void storeBooleanResponse(
      final SurveyResponseDto surveyResponse,
      final User user,
      final SurveyInstance instance,
      final Question question) {

    final Optional<SurveyResponse> entityOp =
        this.surveyResponseRepository.findTopByUserAndSurveyInstanceAndQuestionOrderByVersionDesc(
            user, instance, question);

    final boolean isSkipped = surveyResponse.getSkipped() == null ? false : surveyResponse.getSkipped();

    if (entityOp.isEmpty()) {

      this.surveyResponseRepository.save(SurveyResponse.builder()
          .question(question)
          .surveyInstance(instance)
          .user(user)
          .boolAnswer(isSkipped ? null : surveyResponse.getBoolAnswer())
          .skipped(isSkipped)
          .valid(true)
          .build());

    } else {

      final SurveyResponse entity = entityOp.get();
      // Invalidate existing entity
      this.surveyResponseRepository.save(entity.toBuilder()
          .valid(false)
          .build());
      // Add new version
      this.surveyResponseRepository.save(entity.newVersion()
          .boolAnswer(isSkipped ? null : surveyResponse.getBoolAnswer())
          .skipped(isSkipped)
          .valid(true)
          .build());
    }
  }

  private final void storeChecklistResponse(
      final SurveyResponseDto surveyResponse,
      final User user,
      final SurveyInstance instance,
      final Question question) {

    final ChecklistQuestion checklistQuestion = (ChecklistQuestion) question;

    final List<SurveyResponse> entities =
        this.surveyResponseRepository.findByUserAndSurveyInstanceAndQuestionInOrderByVersionDesc(
            user, instance, checklistQuestion.getEntries());

    final boolean isSkipped = surveyResponse.getSkipped() == null ? false : surveyResponse.getSkipped();

    for (final ChecklistEntry entry : checklistQuestion.getEntries()) {

      final Boolean answer = isSkipped
          ? null
          : surveyResponse.getChecklistAnswer() == null
              ? null
              : surveyResponse.getChecklistAnswer().get(entry.getId());

      final Optional<SurveyResponse> entityOp =
          entities.stream().filter(p -> p.getQuestion().getId().equals(entry.getId())).findFirst();

      if (entityOp.isEmpty()) {

        this.surveyResponseRepository.save(SurveyResponse.builder()
            .question(entry)
            .surveyInstance(instance)
            .user(user)
            .boolAnswer(answer == null ? false : answer)
            .skipped(isSkipped)
            .valid(true)
            .build());

      } else {

        final SurveyResponse entity = entityOp.get();
        // Invalidate existing entity
        this.surveyResponseRepository.save(entity.toBuilder()
            .valid(false)
            .build());
        // Add new version
        this.surveyResponseRepository.save(entity.newVersion()
            .boolAnswer(answer == null ? false : answer)
            .skipped(isSkipped)
            .valid(true)
            .build());
      }
    }
  }

  private final void storeChoiceResponse(
      final SurveyResponseDto surveyResponse,
      final User user,
      final SurveyInstance instance,
      final Question question) {

    final List<Answer> existingAnswers = new ArrayList<>();

    final boolean isSkipped = surveyResponse.getSkipped() == null ? false : surveyResponse.getSkipped();

    if (!isSkipped) {
      for (final Long answerId : surveyResponse.getAnswerIds()) {

        final Optional<Answer> answerOp = this.answerRepository.findById(answerId);

        if (answerOp.isEmpty())
          throw new IllegalStateException("Unexpected state: Could not find answer entity for id: " + answerId);

        existingAnswers.add(answerOp.get());
      }
    }

    final Optional<SurveyResponse> entityOp =
        this.surveyResponseRepository.findTopByUserAndSurveyInstanceAndQuestionOrderByVersionDesc(
            user, instance, question);

    if (entityOp.isEmpty()) {

      this.surveyResponseRepository.save(SurveyResponse.builder()
          .question(question)
          .surveyInstance(instance)
          .user(user)
          .answers(isSkipped ? null : existingAnswers)
          .skipped(isSkipped)
          .valid(true)
          .build());

    } else {

      final SurveyResponse entity = entityOp.get();
      // Invalidate existing entity
      this.surveyResponseRepository.save(entity.toBuilder()
          .valid(false)
          .build());
      // Add new version
      this.surveyResponseRepository.save(entity.newVersion()
          .answers(isSkipped ? null : existingAnswers)
          .skipped(isSkipped)
          .valid(true)
          .build());
    }
  }

  private final void storeNumberResponse(
      final SurveyResponseDto surveyResponse,
      final User user,
      final SurveyInstance instance,
      final Question question) {

    final Optional<SurveyResponse> entityOp =
        this.surveyResponseRepository.findTopByUserAndSurveyInstanceAndQuestionOrderByVersionDesc(
            user, instance, question);

    final boolean isSkipped = surveyResponse.getSkipped() == null ? false : surveyResponse.getSkipped();

    if (entityOp.isEmpty()) {

      this.surveyResponseRepository.save(SurveyResponse.builder()
          .question(question)
          .surveyInstance(instance)
          .user(user)
          .numberAnswer(isSkipped ? null : surveyResponse.getNumberAnswer())
          .skipped(isSkipped)
          .valid(true)
          .build());

    } else {

      final SurveyResponse entity = entityOp.get();
      // Invalidate existing entity
      this.surveyResponseRepository.save(entity.toBuilder()
          .valid(false)
          .build());
      // Add new version
      this.surveyResponseRepository.save(entity.newVersion()
          .numberAnswer(isSkipped ? null : surveyResponse.getNumberAnswer())
          .skipped(isSkipped)
          .valid(true)
          .build());
    }
  }

  private final void storeTextResponse(
      final SurveyResponseDto surveyResponse,
      final User user,
      final SurveyInstance instance,
      final Question question) {

    final Optional<SurveyResponse> entityOp =
        this.surveyResponseRepository.findTopByUserAndSurveyInstanceAndQuestionOrderByVersionDesc(
            user, instance, question);

    final boolean isSkipped = surveyResponse.getSkipped() == null ? false : surveyResponse.getSkipped();

    if (entityOp.isEmpty()) {

      this.surveyResponseRepository.save(SurveyResponse.builder()
          .question(question)
          .surveyInstance(instance)
          .user(user)
          .textAnswer(isSkipped ? null : surveyResponse.getTextAnswer())
          .skipped(isSkipped)
          .valid(true)
          .build());

    } else {

      final SurveyResponse entity = entityOp.get();
      // Invalidate existing entity
      this.surveyResponseRepository.save(entity.toBuilder()
          .valid(false)
          .build());
      // Add new version
      this.surveyResponseRepository.save(entity.newVersion()
          .textAnswer(isSkipped ? null : surveyResponse.getTextAnswer())
          .skipped(isSkipped)
          .valid(true)
          .build());
    }
  }

  private Question getNextSubQuestion(final BooleanQuestion question, final SurveyResponseDto response) {

    if (!question.hasContainer()
        || question.getContainer().getQuestions() == null
        || question.getContainer().getQuestions().isEmpty()
        || question.isOptional() && Boolean.TRUE.equals(response.getSkipped())
        || !question.getContainer().getDependsOn().equals(response.getBoolAnswer()))
      return null;

    return question.getContainer().getQuestions().get(0);
  }

  private Question getNextSubQuestion(final ChoiceQuestion question, final SurveyResponseDto response) {

    if (!question.hasContainer()
        || question.getContainer().getQuestions() == null
        || question.getContainer().getQuestions().isEmpty()
        || question.getContainer().getDependsOn() == null
        || question.getContainer().getDependsOn().isEmpty()
        || question.isOptional() && Boolean.TRUE.equals(response.getSkipped())
        || question.getContainer().getDependsOn().stream().noneMatch(p -> response.getAnswerIds().contains(p.getId())))
      return null;

    return question.getContainer().getQuestions().get(0);
  }
}
