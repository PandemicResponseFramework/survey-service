/**
 *
 */
package one.tracking.framework.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReminderTaskResult {

  public static final ReminderTaskResult NOOP = ReminderTaskResult.builder().state(StateType.CANCELLED).build();

  public enum StateType {
    CANCELLED,
    EXECUTED;
  }

  private String surveyNameId;

  private StateType state;

  private int countNotifications;
  private int countDeviceTokens;

}
