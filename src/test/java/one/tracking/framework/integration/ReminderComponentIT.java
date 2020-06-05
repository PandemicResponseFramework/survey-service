/**
 *
 */
package one.tracking.framework.integration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.AnswersWithDelay;
import org.mockito.internal.stubbing.answers.Returns;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.SendResponse;
import one.tracking.framework.SurveyApplication;
import one.tracking.framework.component.ReminderComponent;
import one.tracking.framework.domain.PushNotificationRequest;
import one.tracking.framework.domain.ReminderTaskResult;
import one.tracking.framework.domain.ReminderTaskResult.StateType;
import one.tracking.framework.service.FirebaseService;

/**
 * @author Marko Voß
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class ReminderComponentIT {

  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  @Autowired
  private ResourceLoader resourceLoader;

  @Test
  public void testReminderComponent() throws Exception {

    final MockBeanInitializer beanInitializer = new MockBeanInitializer(FirebaseService.class);

    final Resource resource = this.resourceLoader.getResource("classpath:application-it.properties");
    final Properties properties = new Properties();
    properties.load(resource.getInputStream());

    // Instance 1
    final ConfigurableApplicationContext ctx1 = new SpringApplicationBuilder(SurveyApplication.class)
        .properties(properties).properties("server.port=8081", "spring.jpa.hibernate.ddl-auto=create-drop")
        .initializers(beanInitializer).build().run();

    // Instance 2
    final ConfigurableApplicationContext ctx2 = new SpringApplicationBuilder(SurveyApplication.class)
        .properties(properties).properties("server.port=8082", "spring.jpa.hibernate.ddl-auto=validate")
        .initializers(beanInitializer).build().run();

    // Add HelperBean to context of instance 1
    final HelperBean helper = ctx1.getBean(HelperBean.class);

    // Execute TEST survey creation
    helper.createSurvey("TEST");

    // Register user and device tokens
    for (int i = 0; i < 10000; i++) {
      helper.addDeviceToken(helper.createUser(null), i + "");
    }

    final int fcmBatchSize = 500;
    // final int fcmDelay = 5 * 1000;
    final int fcmDelay = 0;

    // Mock FirebaseService
    final FirebaseService firebaseService1 = ctx1.getBean(FirebaseService.class);
    final FirebaseService firebaseService2 = ctx2.getBean(FirebaseService.class);

    final SendResponse sendResponseSuccess = Mockito.mock(SendResponse.class);
    Mockito.when(sendResponseSuccess.isSuccessful()).thenReturn(true);
    Mockito.when(sendResponseSuccess.getMessageId()).thenReturn("Ok");

    final FirebaseMessagingException exNotRegistered = Mockito.mock(FirebaseMessagingException.class);
    Mockito.when(exNotRegistered.getErrorCode())
        .thenReturn(ReminderComponent.ERROR_CODE_REGISTRATION_TOKEN_NOT_REGISTERED);

    final FirebaseMessagingException exInvalidToken = Mockito.mock(FirebaseMessagingException.class);
    Mockito.when(exInvalidToken.getErrorCode())
        .thenReturn(ReminderComponent.ERROR_CODE_INVALID_REGISTRATION_TOKEN);

    final FirebaseMessagingException exOther = Mockito.mock(FirebaseMessagingException.class);
    Mockito.when(exOther.getErrorCode())
        .thenReturn("messaging/server-unavailable");

    final SendResponse sendResponseNotRegistered = Mockito.mock(SendResponse.class);
    Mockito.when(sendResponseNotRegistered.isSuccessful()).thenReturn(false);
    Mockito.when(sendResponseNotRegistered.getException()).thenReturn(exNotRegistered);

    final SendResponse sendResponseInvalidToken = Mockito.mock(SendResponse.class);
    Mockito.when(sendResponseInvalidToken.isSuccessful()).thenReturn(false);
    Mockito.when(sendResponseInvalidToken.getException()).thenReturn(exInvalidToken);

    final SendResponse sendResponseOtherError = Mockito.mock(SendResponse.class);
    Mockito.when(sendResponseOtherError.isSuccessful()).thenReturn(false);
    Mockito.when(sendResponseOtherError.getException()).thenReturn(exOther);

    final List<SendResponse> sendResponses = new ArrayList<>(fcmBatchSize);

    int countSuccessPerFcmBatch = 0;
    int countFailPerFcmBatch = 0;

    for (int i = 0; i < fcmBatchSize; i++) {

      if (i % 4 == 0) {
        sendResponses.add(sendResponseSuccess);
        countSuccessPerFcmBatch++;
      } else if (i % 4 == 1) {
        sendResponses.add(sendResponseNotRegistered); // invalid DeviceToken -> do no longer use this token
        countFailPerFcmBatch++;
      } else if (i % 4 == 2) {
        sendResponses.add(sendResponseInvalidToken); // invalid DeviceToken -> do no longer use this token
        countFailPerFcmBatch++;
      } else {
        sendResponses.add(sendResponseOtherError); // failed to send -> keep using the DeviceToken
        countFailPerFcmBatch++;
      }
    }

    final BatchResponse batchResponse = Mockito.mock(BatchResponse.class);
    Mockito.when(batchResponse.getSuccessCount()).thenReturn(countSuccessPerFcmBatch);
    Mockito.when(batchResponse.getFailureCount()).thenReturn(countFailPerFcmBatch);
    Mockito.when(batchResponse.getResponses()).thenReturn(sendResponses);

    Mockito.when(firebaseService1.isAvailable()).thenReturn(true);
    Mockito.when(firebaseService2.isAvailable()).thenReturn(true);

    // Batch size = 1000 & FCM batch size = 500 -> return 2 BatchResponses
    Mockito.when(firebaseService1.sendMessages(any(PushNotificationRequest.class), anyList()))
        .then(new AnswersWithDelay(fcmDelay, new Returns(Arrays.asList(batchResponse, batchResponse))));

    // Batch size = 1000 & FCM batch size = 500 -> return 2 BatchResponses
    Mockito.when(firebaseService2.sendMessages(any(PushNotificationRequest.class), anyList()))
        .then(new AnswersWithDelay(fcmDelay, new Returns(Arrays.asList(batchResponse, batchResponse))));

    final ReminderComponent s1 = ctx1.getBean(ReminderComponent.class);
    final ReminderComponent s2 = ctx2.getBean(ReminderComponent.class);

    /*
     * Test reminder for non-existing survey
     */
    final Future<ReminderTaskResult> future = this.executorService.submit(() -> s1.sendReminder("TEST_NONEXISTANT"));

    ReminderTaskResult result = future.get();

    assertThat(result, is(not(nullValue())));
    assertThat(result, is(equalTo(ReminderTaskResult.NOOP)));

    /*
     * Test locking
     */

    final Future<ReminderTaskResult> future1 = this.executorService.submit(() -> s1.sendReminder("TEST"));
    final Future<ReminderTaskResult> future2 = this.executorService.submit(() -> s2.sendReminder("TEST"));

    await().atMost(Duration.ofMinutes(5)).until(() -> {
      return future1.isDone() && future2.isDone();
    });

    assertThat(future1.isCancelled(), is(false));
    assertThat(future2.isCancelled(), is(false));

    assertThat(future1.get(), is(not(nullValue())));
    assertThat(future2.get(), is(not(nullValue())));

    // Only one execution must be executed
    assertThat(future1.get().getState(), is(not(equalTo(future2.get().getState()))));

    if (future1.get().getState() == StateType.EXECUTED) {

      assertThat(future1.get().getSurveyNameId(), is("TEST"));
      assertThat(future1.get().getCountDeviceTokens(), is(10000));
      assertThat(future1.get().getCountNotifications(), is(2500));

      assertThat(future2.get(), is(equalTo(ReminderTaskResult.NOOP)));

    } else {

      assertThat(future2.get().getSurveyNameId(), is("TEST"));
      assertThat(future2.get().getCountDeviceTokens(), is(10000));
      assertThat(future2.get().getCountNotifications(), is(2500));

      assertThat(future1.get(), is(equalTo(ReminderTaskResult.NOOP)));
    }

    /*
     * Test sending reminders not being performed for already sent reminders
     */

    // Send reminders for TEST again on instance 1
    result = this.executorService.submit(() -> s1.sendReminder("TEST")).get();

    assertThat(result, is(not(nullValue())));
    assertThat(result.getState(), is(StateType.EXECUTED));
    assertThat(result.getCountDeviceTokens(), is(0));
    assertThat(result.getCountNotifications(), is(0));

    // Send reminders for TEST again on instance 2
    result = this.executorService.submit(() -> s2.sendReminder("TEST")).get();

    assertThat(result, is(not(nullValue())));
    assertThat(result.getState(), is(StateType.EXECUTED));
    assertThat(result.getCountDeviceTokens(), is(0));
    assertThat(result.getCountNotifications(), is(0));

    helper.createSurvey("TEST_A");
    helper.createSurvey("TEST_B");

    /*
     * Test locking is not interfering for different surveys
     */

    final Future<ReminderTaskResult> futureA = this.executorService.submit(() -> s1.sendReminder("TEST_A"));
    final Future<ReminderTaskResult> futureB = this.executorService.submit(() -> s2.sendReminder("TEST_B"));

    await().atMost(Duration.ofMinutes(5)).until(() -> {
      return futureA.isDone() && futureB.isDone();
    });

    assertThat(futureA.isCancelled(), is(false));
    assertThat(futureB.isCancelled(), is(false));

    assertThat(futureA.get(), is(not(nullValue())));
    assertThat(futureB.get(), is(not(nullValue())));

    // Different topics -> both must be executed
    assertThat(futureA.get().getState(), is(equalTo(StateType.EXECUTED)));
    assertThat(futureB.get().getState(), is(equalTo(StateType.EXECUTED)));

    assertThat(futureA.get().getSurveyNameId(), is("TEST_A"));
    assertThat(futureB.get().getSurveyNameId(), is("TEST_B"));

    // FIXME: Implement dynamic SendResponses depending on DeviceTokens size: 5000/1250 is not correct

    assertThat(futureA.get().getCountDeviceTokens(), is(5000));
    assertThat(futureA.get().getCountNotifications(), is(1250));

    assertThat(futureB.get().getCountDeviceTokens(), is(5000));
    assertThat(futureB.get().getCountNotifications(), is(1250));

  }
}
