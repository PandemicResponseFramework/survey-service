/**
 *
 */
package one.tracking.framework.exception;

import one.tracking.framework.dto.SurveyResponseConflictType;

/**
 * @author Marko Voß
 *
 */
public class SurveyResponseConflictException extends Exception {

  private static final long serialVersionUID = 5126472991807681824L;

  private final SurveyResponseConflictType conflictType;

  public SurveyResponseConflictException(final SurveyResponseConflictType conflictType) {
    super(conflictType.name());
    this.conflictType = conflictType;
  }

  /**
   * @return the conflictType
   */
  public SurveyResponseConflictType getConflictType() {
    return this.conflictType;
  }
}
