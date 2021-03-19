/**
 *
 */
package one.tracking.framework.service;

import static one.tracking.framework.entity.DataConstants.TOKEN_SURVEY_LENGTH;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import one.tracking.framework.domain.Period;
import one.tracking.framework.domain.SurveyStatusType;
import one.tracking.framework.dto.SurveyStatusDto;
import one.tracking.framework.entity.SurveyInstance;
import one.tracking.framework.entity.SurveyResponse;
import one.tracking.framework.entity.SurveyStatus;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.meta.ReleaseStatusType;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.repo.SurveyInstanceRepository;
import one.tracking.framework.repo.SurveyRepository;
import one.tracking.framework.repo.SurveyResponseRepository;
import one.tracking.framework.repo.SurveyStatusRepository;
import one.tracking.framework.repo.UserRepository;
import one.tracking.framework.support.ServiceUtility;

/**
 * @author Marko Voß
 *
 */
@Service
public class SurveyService {

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

  public Collection<SurveyStatusDto> getSurveyOverview(final String userId) {

    final User user = this.userRepository.findById(userId).get();

    // Use TreeMap to keep order by nameId
    final Map<String, SurveyStatusDto> result = new TreeMap<>();

    for (final Survey survey : this.surveyRepository
        .findByReleaseStatusOrderByNameIdAscVersionDesc(ReleaseStatusType.RELEASED)) {

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

    final List<SurveyResponse> surveyResponses =
        this.surveyResponseRepository.findByUserAndSurveyInstanceAndMaxVersion(user, instance);

    final SurveyStatusType status = this.utility.calculateSurveyStatus(survey, surveyResponses);

    return SurveyStatusDto.builder()
        .nameId(survey.getNameId())
        .dependsOn(survey.getDependsOn() == null ? null : survey.getDependsOn())
        .status(status)
        .title(survey.getTitle())
        .description(survey.getDescription())
        .countQuestions(survey.getQuestions().size())
        .nextQuestionId(nextQuestionId)
        .token(instance.getToken())
        .startTime(Period.INFINITE.getStart().equals(instance.getStartTime())
            ? null
            : instance.getStartTime().toEpochMilli())
        .endTime(Period.INFINITE.getEnd().equals(instance.getEndTime())
            ? null
            : instance.getEndTime().toEpochMilli())
        .build();
  }

  /**
   * Self-healing implementation: If the current survey instance does not yet exist, it will be
   * created on request.
   *
   * @param survey
   * @return
   */
  public SurveyInstance getCurrentInstance(final Survey survey) {

    return getInstance(survey, this.utility.getCurrentSurveyInstancePeriod(survey));
  }

  private SurveyInstance getInstance(final Survey survey, final Period period) {

    final Optional<SurveyInstance> instanceOp =
        this.surveyInstanceRepository.findBySurveyAndStartTimeAndEndTime(survey, period.getStart(), period.getEnd());

    if (instanceOp.isPresent())
      return instanceOp.get();

    return this.surveyInstanceRepository.save(SurveyInstance.builder()
        .survey(survey)
        .startTime(period.getStart())
        .endTime(period.getEnd())
        .token(this.utility.generateString(TOKEN_SURVEY_LENGTH))
        .build());
  }
}
