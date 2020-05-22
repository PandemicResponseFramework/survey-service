/**
 *
 */
package one.tracking.framework.component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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

  public boolean sendReminder(final String topic) {

    LOG.debug("Executing scheduled job '{}{}' @ {}", TASK_REMINDER_PREFIX, topic, Instant.now());

    try {

      if (lockAndSendReminder(topic)) {
        LOG.debug("Executing scheduled job '{}{}' DONE @ {}", TASK_REMINDER_PREFIX, topic, Instant.now());
        return true;

      } else {
        LOG.debug("Executing scheduled job '{}{}' CANCELLED @ {}", TASK_REMINDER_PREFIX, topic, Instant.now());
      }

    } catch (final Exception e) {
      LOG.error(e.getMessage(), e);
    }

    return false;
  }

  private boolean lockAndSendReminder(final String topic) {

    try {

      if (this.locker.lock(TASK_REMINDER_PREFIX + topic)) {
        performSendReminder();
        this.locker.free(TASK_REMINDER_PREFIX + topic);
        return true;
      }

    } catch (final DataIntegrityViolationException e) {
      // In case of concurrency by multiple instances, failing to store the same entry is valid
      // Unique index or primary key violation is to be expected
      LOG.debug("Expected violation: {}", e.getMessage());
    }

    return false;
  }

  private void performSendReminder() {

    LOG.debug("Sending reminders...");

    final Instant now = Instant.now();

    final List<String> handledNameIds = new ArrayList<>();

    final List<Survey> surveys =
        this.surveyRepository.findAllByReleaseStatusAndReminderTypeNotAndIntervalTypeNotOrderByNameIdAscVersionDesc(
            ReleaseStatusType.RELEASED,
            ReminderType.NONE,
            IntervalType.NONE);

    for (final Survey survey : surveys) {

      // Handle the current version only
      if (handledNameIds.contains(survey.getNameId()))
        continue;

      handledNameIds.add(survey.getNameId());

      final ChronoUnit unit = survey.getReminderType().toChronoUnit();

      if (unit == null) {
        LOG.error("No mapping defined for reminder type: {}! Skipping sending reminders for survey: {}.",
            survey.getReminderType(), survey.getNameId());
        continue;
      }

      final SurveyInstance instance = this.surveyService.getCurrentInstance(survey);

      if (now.isAfter(instance.getStartTime().plus(survey.getReminderValue(), unit))) {

        performSendReminder(survey, instance);
      }
    }
  }

  /**
   * @param survey
   * @param instance
   */
  private void performSendReminder(final Survey survey, final SurveyInstance instance) {

    for (final DeviceToken deviceToken : this.deviceTokenRepository.findAll()) {

      // Skip if answer of user for the current survey instance exists
      if (this.surveyResponseRepository.existsByUserAndSurveyInstance(deviceToken.getUser(), instance))
        continue;

      // Skip if reminder got sent already
      if (this.reminderRepository.existsByDeviceTokenAndSurveyInstance(deviceToken, instance))
        continue;

      try {

        this.firebaseService.sendMessageToUser(PushNotificationRequest.builder()
            .title(this.reminderTitle)
            .message(this.reminderMessage)
            .data(Collections.singletonMap("surveyNameId", survey.getNameId()))
            .build());

        this.reminderRepository.save(Reminder.builder()
            .deviceToken(deviceToken)
            .surveyInstance(instance)
            .build());

      } catch (InterruptedException | ExecutionException e) {
        LOG.error(e.getMessage(), e);
      }
    }
  }
}
