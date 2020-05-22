/**
 *
 */
package one.tracking.framework.entity.meta;

import java.time.temporal.ChronoUnit;

/**
 * @author Marko Voß
 *
 */
public enum ReminderType {

  NONE,
  AFTER_DAYS;

  public ChronoUnit toChronoUnit() {
    switch (this) {
      case AFTER_DAYS:
        return ChronoUnit.DAYS;
      default:
        return null;
    }
  }

}
