/**
 *
 */
package one.tracking.framework.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.sendgrid.SendGrid;

/**
 * @author Marko Voß
 *
 */
@Configuration
public class MailConfig {

  @Value("${app.sendgrid.api.key}")
  private String sendGridAPIKey;

  @Bean
  SendGrid sendGridClient() {
    return new SendGrid(this.sendGridAPIKey);
  }
}
