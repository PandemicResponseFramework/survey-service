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
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
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
public class FCMSchedulerIT {

  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  @Autowired
  private ResourceLoader resourceLoader;

  @Test
  public void runTest() throws Exception {

    final MockBeanInitializer beanInitializer = MockBeanInitializer.builder()
        .mockedBeans(FirebaseService.class)
        .build();

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

    final int countDeviceTokens = 10000;

    // Mock FirebaseService
    final FirebaseService firebaseService1 = ctx1.getBean(FirebaseService.class);
    final FirebaseService firebaseService2 = ctx2.getBean(FirebaseService.class);

    Mockito.when(firebaseService1.sendMessageToUser(ArgumentMatchers.any(PushNotificationRequest.class)))
        .thenReturn(true);
    Mockito.when(firebaseService2.sendMessageToUser(ArgumentMatchers.any(PushNotificationRequest.class)))
        .thenReturn(true);

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

    await().until(() -> {
      return future1.isDone() && future2.isDone();
    });

    assertThat(future1.get(), is(not(nullValue())));
    assertThat(future2.get(), is(not(nullValue())));

    // Only one execution must be executed
    assertThat(future1.get().getState(), is(not(equalTo(future2.get().getState()))));

    if (future1.get().getState() == StateType.EXECUTED) {

      assertThat(future1.get().getSurveyNameId(), is("TEST"));
      assertThat(future1.get().getCountDeviceTokens(), is(countDeviceTokens));
      assertThat(future1.get().getCountNotifications(), is(countDeviceTokens));

      assertThat(future2.get(), is(equalTo(ReminderTaskResult.NOOP)));

    } else {

      assertThat(future2.get().getSurveyNameId(), is("TEST"));
      assertThat(future2.get().getCountDeviceTokens(), is(countDeviceTokens));
      assertThat(future2.get().getCountNotifications(), is(countDeviceTokens));

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

    await().until(() -> {
      return futureA.isDone() && futureB.isDone();
    });

    // Different topics -> both must be true
    assertThat(futureA.get(), is(not(nullValue())));
    assertThat(futureB.get(), is(not(nullValue())));

    assertThat(futureA.get().getState(), is(equalTo(StateType.EXECUTED)));
    assertThat(futureB.get().getState(), is(equalTo(StateType.EXECUTED)));

    assertThat(futureA.get().getSurveyNameId(), is("TEST_A"));
    assertThat(futureB.get().getSurveyNameId(), is("TEST_B"));

    assertThat(futureA.get().getCountDeviceTokens(), is(countDeviceTokens));
    assertThat(futureA.get().getCountNotifications(), is(countDeviceTokens));

    assertThat(futureB.get().getCountDeviceTokens(), is(countDeviceTokens));
    assertThat(futureB.get().getCountNotifications(), is(countDeviceTokens));

  }
}
