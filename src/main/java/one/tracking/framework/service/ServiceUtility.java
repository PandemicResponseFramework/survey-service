/**
 *
 */
package one.tracking.framework.service;

import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * @author Marko Voß
 *
 */
@Component
public final class ServiceUtility {

  private static final Random RANDOM = new Random();

  /**
   *
   * @param length
   * @return
   */
  public String generateString(final int length) {
    final int leftLimit = 48; // numeral '0'
    final int rightLimit = 122; // letter 'z'

    final String generatedString = RANDOM.ints(leftLimit, rightLimit + 1)
        .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
        .limit(length)
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();

    return generatedString;
  }
}
