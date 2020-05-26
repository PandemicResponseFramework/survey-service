/**
 *
 */
package one.tracking.framework.domain;

import java.util.Map;
import lombok.Builder;
import lombok.Data;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
public class PushNotificationRequest {

  private String title;
  private String message;
  private String topic;
  private Map<String, String> data;
  private String token;
}
