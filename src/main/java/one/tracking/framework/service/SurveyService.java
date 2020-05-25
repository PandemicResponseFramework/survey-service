/**
 *
 */
package one.tracking.framework.service;

import static one.tracking.framework.entity.DataConstants.TOKEN_SURVEY_LENGTH;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import one.tracking.framework.dto.SurveyStatusDto;
import one.tracking.framework.dto.SurveyStatusType;
import one.tracking.framework.entity.SurveyInstance;
import one.tracking.framework.entity.SurveyResponse;
import one.tracking.framework.entity.SurveyStatus;
import one.tracking.framework.entity.User;
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

/**
 * @author Marko Voß
 *
 */
@Service
public class SurveyService {

  private static final Logger LOG = LoggerFactory.getLogger(SurveyService.class);

  public static final Instant INSTANT_MIN = Instant.ofEpochMilli(0);
  // FIXME: Long.MAX_VALUE causes overflow on DB -> beware Christmas in 9999!
  public static final Instant INSTANT_MAX = Instant.parse("9999-12-24T00:00:00Z");

  @Autowired
  private ServiceUtility utility;

  @Autowired
  private SurveyRepository surveyRepository;

  @Autowired
  private SurveyInstanceRepository surveyInstanceRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private SurveyResponseRepository surveyResponseRepository;

  @Autowired
  private SurveyStatusRepository surveyStatusRepository;

  public Survey getReleasedSurvey(final String nameId) {

    return this.surveyRepository.findTopByNameIdAndReleaseStatusOrderByVersionDesc(nameId, ReleaseStatusType.RELEASED)
        .get();
  }

  public SurveyStatusDto getSurveyOverview(final String nameId, final String userId) {

    final User user = this.userRepository.findById(userId).get();

    final Optional<Survey> surveyOp =
        this.surveyRepository.findTopByNameIdAndReleaseStatusOrderByVersionDesc(nameId, ReleaseStatusType.RELEASED);

    if (surveyOp.isEmpty())
      return null;

    return getStatus(surveyOp.get(), user);
  }

  /**
   * TODO: Currently the interval logic is very limited and needs to be implemented as a generic
   * approach later.
   *
   */
  public Collection<SurveyStatusDto> getSurveyOverview(final String userId) {

    final User user = this.userRepository.findById(userId).get();

    // Use TreeMap to keep order by nameId
    final Map<String, SurveyStatusDto> result = new TreeMap<>();

    for (final Survey survey : this.surveyRepository
        .findAllByReleaseStatusOrderByNameIdAscVersionDesc(ReleaseStatusType.RELEASED)) {

      // Collect each survey only once by its top most released version
      if (result.get(survey.getNameId()) != null)
        continue;

      final SurveyStatusDto status = getStatus(survey, user);
      if (status != null)
        result.put(survey.getNameId(), status);
    }

    return result.values();
  }

  private SurveyStatusDto getStatus(final Survey survey, final User user) {

    final SurveyInstance instance = getCurrentInstance(survey);

    if (instance == null)
      return null;

    final Optional<SurveyStatus> surveyStatusOp =
        this.surveyStatusRepository.findByUserAndSurveyInstance(user, instance);

    Long nextQuestionId = null;

    if (surveyStatusOp.isPresent()) {
      final SurveyStatus surveyStatus = surveyStatusOp.get();
      nextQuestionId = surveyStatus.getNextQuestion() == null ? null : surveyStatus.getNextQuestion().getId();
    }

    final SurveyStatusType status = calculateSurveyStatus(user, instance);

    return SurveyStatusDto.builder()
        .nameId(survey.getNameId())
        .status(status)
        .title(survey.getTitle())
        .description(survey.getDescription())
        .countQuestions(survey.getQuestions().size())
        .nextQuestionId(nextQuestionId)
        .token(instance.getToken())
        .startTime(INSTANT_MIN.equals(instance.getStartTime()) ? null : instance.getStartTime().toEpochMilli())
        .endTime(INSTANT_MAX.equals(instance.getEndTime()) ? null : instance.getEndTime().toEpochMilli())
        .build();
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
      case NUMBER:
      case RANGE: {

        final SurveyResponse response = responses.get(question.getId());
        return response != null && response.getNumberAnswer() != null;

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
  public SurveyInstance getCurrentInstance(final Survey survey) {

    switch (survey.getIntervalType()) {
      case WEEKLY:
        return getInstanceByWeek(survey);
      case NONE:
      default:
        return getPermanentInstance(survey);
    }
  }

  private SurveyInstance getInstanceByWeek(final Survey survey) {

    final ZonedDateTime start = survey.getIntervalStart().atZone(ZoneOffset.UTC);
    final ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

    if (start.isAfter(now))
      return null;

    final int weekStart = start.get(WeekFields.ISO.weekOfWeekBasedYear());
    final int weekNow = now.get(WeekFields.ISO.weekOfWeekBasedYear());

    final int weekDelta = (int) (Math.floor((weekNow - weekStart) / (double) survey.getIntervalValue()));

    final ZonedDateTime startTime = start.plusWeeks(weekDelta * survey.getIntervalValue());
    final ZonedDateTime endTime = startTime.plus(survey.getIntervalValue(), survey.getIntervalType().toChronoUnit())
        .minusSeconds(1);

    return getInstance(survey, startTime.toInstant(), endTime.toInstant());
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
        .token(this.utility.generateString(TOKEN_SURVEY_LENGTH))
        .build());
  }
}
