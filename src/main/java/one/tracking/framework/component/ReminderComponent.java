/**
 *
 */
package one.tracking.framework.component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import one.tracking.framework.domain.PushNotificationRequest;
import one.tracking.framework.domain.ReminderTaskResult;
import one.tracking.framework.domain.ReminderTaskResult.StateType;
import one.tracking.framework.entity.DeviceToken;
import one.tracking.framework.entity.Reminder;
import one.tracking.framework.entity.SurveyInstance;
import one.tracking.framework.entity.meta.IntervalType;
import one.tracking.framework.entity.meta.ReleaseStatusType;
import one.tracking.framework.entity.meta.ReminderType;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.repo.DeviceTokenRepository;
import one.tracking.framework.repo.ReminderRepository;
import one.tracking.framework.repo.SurveyRepository;
import one.tracking.framework.repo.SurveyResponseRepository;
import one.tracking.framework.service.FirebaseService;
import one.tracking.framework.service.SurveyService;

/**
 * @author Marko Voß
 *
 */
@Component
public class ReminderComponent {

  private static final Logger LOG = LoggerFactory.getLogger(ReminderComponent.class);

  private static final String TASK_REMINDER_PREFIX = "REMINDER_";

  @Autowired
  private DeviceTokenRepository deviceTokenRepository;

  @Autowired
  private SurveyResponseRepository surveyResponseRepository;

  @Autowired
  private ReminderRepository reminderRepository;

  @Autowired
  private SurveyService surveyService;

  @Autowired
  private SurveyRepository surveyRepository;

  @Autowired
  private FirebaseService firebaseService;

  @Autowired
  private LockerComponent locker;

  @Value("${app.reminder.title}")
  private String reminderTitle;

  @Value("${app.reminder.message}")
  private String reminderMessage;

  public ReminderTaskResult sendReminder(final String nameId) {

    LOG.debug("Executing scheduled job '{}{}'", TASK_REMINDER_PREFIX, nameId);

    try {

      final ReminderTaskResult result = lockAndSendReminder(nameId);
      if (result == ReminderTaskResult.NOOP)
        LOG.debug("Executing scheduled job '{}{}' CANCELLED", TASK_REMINDER_PREFIX, nameId);
      else
        LOG.debug("Executing scheduled job '{}{}' DONE", TASK_REMINDER_PREFIX, nameId);

      return result;

    } catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      return ReminderTaskResult.NOOP;
    }
  }

  private ReminderTaskResult lockAndSendReminder(final String nameId) {

    try {

      if (this.locker.lock(TASK_REMINDER_PREFIX + nameId)) {
        final ReminderTaskResult result = performSendReminder(nameId);
        this.locker.free(TASK_REMINDER_PREFIX + nameId);
        return result;
      }

    } catch (final DataIntegrityViolationException e) {
      // In case of concurrency by multiple instances, failing to store the same entry is valid
      // Unique index or primary key violation is to be expected
      LOG.debug("Expected violation: {}", e.getMessage());
    }

    return ReminderTaskResult.NOOP;
  }

  private ReminderTaskResult performSendReminder(final String nameId) {

    LOG.debug("Sending reminders for survey '{}'...", nameId);

    final Instant now = Instant.now();

    final List<Survey> surveys =
        this.surveyRepository
            .findAllByNameIdAndReleaseStatusAndReminderTypeNotAndIntervalTypeNotOrderByNameIdAscVersionDesc(
                nameId,
                ReleaseStatusType.RELEASED,
                ReminderType.NONE,
                IntervalType.NONE);

    if (surveys.isEmpty())
      return ReminderTaskResult.NOOP;

    final Survey currentRelease = surveys.get(0);

    final ChronoUnit unit = currentRelease.getReminderType().toChronoUnit();

    if (unit == null) {
      throw new RuntimeException(
          "No mapping defined for reminder type: " + currentRelease.getReminderType()
              + "! Skipping sending reminders for survey: " + nameId);
    }

    final SurveyInstance instance = this.surveyService.getCurrentInstance(currentRelease);

    if (now.isAfter(instance.getStartTime().plus(currentRelease.getReminderValue(), unit))) {

      return performSendReminder(currentRelease, instance);
    }

    return ReminderTaskResult.builder()
        .surveyNameId(nameId)
        .state(StateType.EXECUTED).build();
  }

  /**
   * @param survey
   * @param instance
   * @return
   */
  private ReminderTaskResult performSendReminder(final Survey survey, final SurveyInstance instance) {

    int countNotifications = 0;
    int countDeviceTokens = 0;

    for (final DeviceToken deviceToken : this.deviceTokenRepository.findAll()) {

      // Skip if answer of user for the current survey instance exists
      if (this.surveyResponseRepository.existsByUserAndSurveyInstance(deviceToken.getUser(), instance))
        continue;

      // Skip if reminder got sent already
      if (this.reminderRepository.existsByDeviceTokenAndSurveyInstance(deviceToken, instance))
        continue;

      try {

        final boolean result = this.firebaseService.sendMessageToUser(PushNotificationRequest.builder()
            .title(this.reminderTitle)
            .message(this.reminderMessage)
            .data(Collections.singletonMap("surveyNameId", survey.getNameId()))
            .build());

        if (result)
          countNotifications++;
        countDeviceTokens++;

        this.reminderRepository.save(Reminder.builder()
            .deviceToken(deviceToken)
            .surveyInstance(instance)
            .build());

      } catch (InterruptedException | ExecutionException e) {
        LOG.error(e.getMessage(), e);
      }
    }

    return ReminderTaskResult.builder()
        .surveyNameId(survey.getNameId())
        .state(StateType.EXECUTED)
        .countDeviceTokens(countDeviceTokens)
        .countNotifications(countNotifications)
        .build();
  }
}
