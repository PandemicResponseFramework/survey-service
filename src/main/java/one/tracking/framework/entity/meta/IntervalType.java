/**
 *
 */
package one.tracking.framework.entity.meta;

import java.time.temporal.ChronoUnit;

/**
 * Very simple interval definition.
 *
 * @author Marko Voß
 *
 */
public enum IntervalType {

  NONE,
  WEEKLY;

  public ChronoUnit toChronoUnit() {
    switch (this) {
      case WEEKLY:
        return ChronoUnit.WEEKS;
      default:
        return null;
    }
  }
}
