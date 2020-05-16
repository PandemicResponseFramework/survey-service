/**
 *
 */
package one.tracking.framework.exception;

/**
 * @author Marko Voß
 *
 */
public class ConflictException extends RuntimeException {

  private static final long serialVersionUID = 5400770108840274632L;

  public ConflictException() {
    super();
  }

  public ConflictException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public ConflictException(final String s) {
    super(s);
  }

  public ConflictException(final Throwable cause) {
    super(cause);
  }
}
