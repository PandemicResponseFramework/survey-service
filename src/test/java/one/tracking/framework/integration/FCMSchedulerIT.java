/**
 *
 */
package one.tracking.framework.integration;

import static org.awaitility.Awaitility.await;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import one.tracking.framework.SurveyApplication;
import one.tracking.framework.config.SchedulerConfig;

/**
 * @author Marko Voß
 *
 */
@TestPropertySource(
    locations = "classpath:application-it.properties")
@RunWith(SpringJUnit4ClassRunner.class)
public class FCMSchedulerIT {

  private final ExecutorService executorService = Executors.newFixedThreadPool(10);

  @Test
  public void runTest() throws Exception {

    // Instance 1
    final ConfigurableApplicationContext ctx1 = new SpringApplicationBuilder(SurveyApplication.class)
        .properties("server.port=8081").build().run();

    // Instance 2
    final ConfigurableApplicationContext ctx2 = new SpringApplicationBuilder(SurveyApplication.class)
        .properties("server.port=8082", "spring.jpa.hibernate.ddl-auto=validate").build().run();

    final SchedulerConfig config1 = ctx1.getBean(SchedulerConfig.class);
    final SchedulerConfig config2 = ctx2.getBean(SchedulerConfig.class);

    final Future<?> future1 = this.executorService.submit(() -> config1.sendReminder());
    final Future<?> future2 = this.executorService.submit(() -> config2.sendReminder());

    await().until(() -> {
      return future1.isDone() && future2.isDone();
    });
  }
}
