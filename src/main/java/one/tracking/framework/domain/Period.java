/**
 *
 */
package one.tracking.framework.domain;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Period implementation supporting {@link Instant} values.
 *
 * @author Marko Voß
 *
 */
@Data
@Builder
@AllArgsConstructor
public class Period {

  // FIXME: Long.MAX_VALUE causes overflow on DB -> beware Christmas in 9999!
  public static final Period INFINITE = new Period(Instant.ofEpochMilli(0), Instant.parse("9999-12-24T00:00:00Z"));

  private Instant start;
  private Instant end;

}
