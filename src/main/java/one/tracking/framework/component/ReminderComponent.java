/**
 *
 */
package one.tracking.framework.component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.SendResponse;
import one.tracking.framework.domain.PushNotificationRequest;
import one.tracking.framework.domain.ReminderBatchResult;
import one.tracking.framework.domain.ReminderTaskResult;
import one.tracking.framework.domain.ReminderTaskResult.StateType;
import one.tracking.framework.entity.DeviceToken;
import one.tracking.framework.entity.Reminder;
import one.tracking.framework.entity.SurveyInstance;
import one.tracking.framework.entity.meta.IntervalType;
import one.tracking.framework.entity.meta.ReleaseStatusType;
import one.tracking.framework.entity.meta.ReminderType;
import one.tracking.framework.entity.meta.Survey;
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

  public static final String ERROR_CODE_REGISTRATION_TOKEN_NOT_REGISTERED =
      "messaging/registration-token-not-registered";

  public static final String ERROR_CODE_INVALID_REGISTRATION_TOKEN =
      "messaging/invalid-registration-token";

  private static final Logger LOG = LoggerFactory.getLogger(ReminderComponent.class);

  private static final String TASK_REMINDER_PREFIX = "REMINDER_";

  private static final String KEY_SURVEY_NAME_ID = "surveyNameId";

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

  @Autowired
  private JpaContext jpaContext;

  @Value("${app.reminder.title}")
  private String reminderTitle;

  @Value("${app.reminder.message}")
  private String reminderMessage;

  @Value("${app.task.reminder.batchSize:1000}")
  private int batchSize;

  @Transactional(propagation = Propagation.REQUIRED)
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
      LOG.debug("Executing scheduled job '{}{}' ERROR", TASK_REMINDER_PREFIX, nameId);
      LOG.error(e.getMessage(), e);
      return ReminderTaskResult.NOOP;
    }
  }

  private ReminderTaskResult lockAndSendReminder(final String nameId) throws InterruptedException, ExecutionException {

    boolean locked;

    try {
      locked = this.locker.lock(TASK_REMINDER_PREFIX + nameId);

    } catch (final DataIntegrityViolationException e) {
      // In case of concurrency by multiple instances, failing to store the same entry is valid
      // Unique index or primary key violation is to be expected
      LOG.debug("Expected violation: {}", e.getMessage());
      return ReminderTaskResult.NOOP;
    }

    if (locked) {

      final ReminderTaskResult result = performSendReminder(nameId);
      this.locker.free(TASK_REMINDER_PREFIX + nameId);
      return result;
    }

    return ReminderTaskResult.NOOP;
  }

  private ReminderTaskResult performSendReminder(final String nameId) throws InterruptedException, ExecutionException {

    LOG.debug("Sending reminders for survey '{}'...", nameId);

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
    final SurveyInstance instance = this.surveyService.getCurrentInstance(currentRelease);

    if (instance == null)
      return ReminderTaskResult.NOOP;

    return performSendReminderNEW(currentRelease, instance);
  }

  private ReminderTaskResult performSendReminderNEW(final Survey survey, final SurveyInstance instance)
      throws InterruptedException, ExecutionException {

    final Instant now = Instant.now();
    final int batchSize = 1000;

    int successCount = 0;
    int tokenCount = 0;
    int offset = 0;

    final EntityManager deviceTokenManager = this.jpaContext.getEntityManagerByManagedType(DeviceToken.class);
    final EntityManager reminderManager = this.jpaContext.getEntityManagerByManagedType(Reminder.class);

    final Session deviceTokenSession = deviceTokenManager.unwrap(Session.class);
    final Session reminderSession = reminderManager.unwrap(Session.class);

    final TypedQuery<DeviceToken> query = deviceTokenSession.createQuery(
        "SELECT d FROM DeviceToken d WHERE d.createdAt < ?1 ORDER BY d.id ASC", DeviceToken.class);
    query.setFirstResult(offset);
    query.setMaxResults(this.batchSize);
    query.setParameter(1, now);

    Transaction deviceTokenTx = deviceTokenSession.isJoinedToTransaction() ? deviceTokenSession.getTransaction()
        : deviceTokenSession.beginTransaction();

    List<DeviceToken> deviceTokens = null;
    while (!(deviceTokens = query.getResultList()).isEmpty()) {

      deviceTokenTx.commit();

      LOG.debug("{}: DeviceToken pages: Offset {} | Batch Size: {} | Page Size: {}",
          survey.getNameId(),
          offset,
          batchSize,
          deviceTokens.size());

      final List<DeviceToken> inactiveDeviceTokens = collectInactiveDeviceTokens(instance, deviceTokens);

      if (inactiveDeviceTokens.isEmpty()) {

        LOG.debug("{}: No DeviceTokens available to send messages to. Skipping sending messages.", survey.getNameId());
        offset += batchSize;

      } else {

        final ReminderBatchResult batchResponse = performSendReminderBatch(inactiveDeviceTokens, survey, instance);

        if (!batchResponse.getInvalidDeviceTokens().isEmpty()) {

          removeInvalidDeviceTokens(survey, deviceTokenSession, reminderSession,
              batchResponse.getInvalidDeviceTokens());

          offset += batchResponse.getInvalidDeviceTokens().size();

        } else {
          offset += batchSize;
        }

        if (!batchResponse.getValidDeviceTokens().isEmpty())
          persistSentReminders(survey, instance, reminderSession, batchResponse);

        successCount += batchResponse.getBatchResponses().stream().mapToInt(f -> f.getSuccessCount()).sum();
        tokenCount += inactiveDeviceTokens.size();

      }

      query.setFirstResult(offset);
      deviceTokenTx = deviceTokenSession.isJoinedToTransaction() ? deviceTokenSession.getTransaction()
          : deviceTokenSession.beginTransaction();
    }

    return ReminderTaskResult.builder()
        .countDeviceTokens(tokenCount)
        .countNotifications(successCount)
        .state(StateType.EXECUTED)
        .surveyNameId(survey.getNameId())
        .build();
  }

  private void removeInvalidDeviceTokens(final Survey survey, final Session deviceTokenSession,
      final Session reminderSession, final List<DeviceToken> deviceTokens) {

    LOG.debug("{}: Invalid DeviceTokens: {}", survey.getNameId(), deviceTokens.size());

    final List<Long> ids = deviceTokens.stream().map(m -> m.getId()).collect(Collectors.toList());

    final Transaction reminderTx =
        reminderSession.isJoinedToTransaction() ? reminderSession.getTransaction()
            : reminderSession.beginTransaction();

    final Query deleteReminderQuery =
        reminderSession.createQuery("DELETE FROM Reminder r WHERE r.deviceToken.id IN (?1)");
    deleteReminderQuery.setParameter(1, ids);
    deleteReminderQuery.executeUpdate();

    reminderTx.commit();

    final Transaction deviceTokenTx = deviceTokenSession.isJoinedToTransaction() ? deviceTokenSession.getTransaction()
        : deviceTokenSession.beginTransaction();

    final Query deleteDeviceTokenQuery =
        deviceTokenSession.createQuery("DELETE FROM DeviceToken d WHERE d.id IN (?1)");
    deleteDeviceTokenQuery.setParameter(1, ids);
    deleteDeviceTokenQuery.executeUpdate();

    deviceTokenTx.commit();
  }

  private void persistSentReminders(final Survey survey, final SurveyInstance instance, final Session reminderSession,
      final ReminderBatchResult batchResponse) {

    LOG.debug("{}: Valid DeviceTokens: {}", survey.getNameId(), batchResponse.getValidDeviceTokens().size());

    final Transaction reminderTx =
        reminderSession.isJoinedToTransaction() ? reminderSession.getTransaction()
            : reminderSession.beginTransaction();

    for (final DeviceToken deviceToken : batchResponse.getValidDeviceTokens()) {

      reminderSession.persist(Reminder.builder()
          .deviceToken(deviceToken)
          .surveyInstance(instance)
          .build());
    }

    reminderTx.commit();
  }

  private List<DeviceToken> collectInactiveDeviceTokens(final SurveyInstance instance,
      final Iterable<DeviceToken> deviceTokens) {

    final List<DeviceToken> result = new ArrayList<>();

    for (final DeviceToken deviceToken : deviceTokens) {

      // Skip if answer of user for the current survey instance exists
      if (this.surveyResponseRepository.existsByUserAndSurveyInstance(deviceToken.getUser(), instance))
        continue;

      // Skip if reminder got sent already
      if (this.reminderRepository.existsByDeviceTokenAndSurveyInstance(deviceToken, instance))
        continue;

      result.add(deviceToken);
    }

    return result;
  }

  private ReminderBatchResult performSendReminderBatch(final List<DeviceToken> deviceTokens, final Survey survey,
      final SurveyInstance instance) throws InterruptedException, ExecutionException {

    final List<BatchResponse> batchResponses = this.firebaseService.sendMessages(PushNotificationRequest.builder()
        .title(this.reminderTitle)
        .message(this.reminderMessage)
        .data(Collections.singletonMap(KEY_SURVEY_NAME_ID, survey.getNameId()))
        .build(),
        deviceTokens.stream().map(m -> m.getToken()).collect(Collectors.toList()));

    final List<DeviceToken> validDeviceTokens = new ArrayList<>();
    final List<DeviceToken> invalidDeviceTokens = new ArrayList<>();

    final List<SendResponse> sendResponses =
        batchResponses.stream().flatMap(f -> f.getResponses().stream()).collect(Collectors.toList());

    // Check, which DeviceTokens are in-/valid
    for (int i = 0; i < deviceTokens.size(); i++) {

      final SendResponse response = sendResponses.get(i);
      final DeviceToken currentToken = deviceTokens.get(i);

      if (!response.isSuccessful()) {

        if (ERROR_CODE_INVALID_REGISTRATION_TOKEN.equals(response.getException().getErrorCode())
            || ERROR_CODE_REGISTRATION_TOKEN_NOT_REGISTERED.equals(response.getException().getErrorCode())) {

          LOG.trace("{}: Invalid DeviceToken: {}", survey.getNameId(), currentToken);
          invalidDeviceTokens.add(currentToken);

        } else {

          LOG.warn("{}: Sending message to DeviceToken '{}' failed. Error code: {}", survey.getNameId(),
              currentToken.getToken(), response.getException().getErrorCode());
          validDeviceTokens.add(currentToken);
        }
      } else {

        LOG.trace("{}: Message send to DeviceToken: {}", survey.getNameId(), currentToken);
        validDeviceTokens.add(currentToken);
      }
    }

    return ReminderBatchResult.builder()
        .batchResponses(batchResponses)
        .invalidDeviceTokens(invalidDeviceTokens)
        .validDeviceTokens(validDeviceTokens)
        .build();
  }
}
