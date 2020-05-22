/**
 *
 */
package one.tracking.framework.integration;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import one.tracking.framework.SurveyApplication;
import one.tracking.framework.component.ReminderComponent;

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

    final Resource resource = this.resourceLoader.getResource("classpath:application-it.properties");
    final Properties properties = new Properties();
    properties.load(resource.getInputStream());

    // Instance 1
    final ConfigurableApplicationContext ctx1 = new SpringApplicationBuilder(SurveyApplication.class)
        .properties(properties).properties("server.port=8081", "spring.jpa.hibernate.ddl-auto=create-drop")
        .build().run();

    // Instance 2
    final ConfigurableApplicationContext ctx2 = new SpringApplicationBuilder(SurveyApplication.class)
        .properties(properties).properties("server.port=8082", "spring.jpa.hibernate.ddl-auto=validate")
        .build().run();

    final ReminderComponent s1 = ctx1.getBean(ReminderComponent.class);
    final ReminderComponent s2 = ctx2.getBean(ReminderComponent.class);

    final Future<Boolean> future1 = this.executorService.submit(() -> s1.sendReminder("TEST"));
    final Future<Boolean> future2 = this.executorService.submit(() -> s2.sendReminder("TEST"));

    await().until(() -> {
      return future1.isDone() && future2.isDone();
    });

    // Only one execution must return true
    assertThat(future1.get(), is(not(equalTo(future2.get()))));

    final Future<Boolean> futureA = this.executorService.submit(() -> s1.sendReminder("TESTA"));
    final Future<Boolean> futureB = this.executorService.submit(() -> s2.sendReminder("TESTB"));

    await().until(() -> {
      return futureA.isDone() && futureB.isDone();
    });

    // Different topics -> both must be true
    assertThat(futureA.get(), is(true));
    assertThat(futureB.get(), is(true));
  }
}
