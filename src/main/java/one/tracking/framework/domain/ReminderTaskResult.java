/**
 *
 */
package one.tracking.framework.domain;

import lombok.Builder;
import lombok.Data;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
public class ReminderTaskResult {

  private Integer counter;
}
