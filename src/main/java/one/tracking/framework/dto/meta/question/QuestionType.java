/**
 *
 */
package one.tracking.framework.dto.meta.question;

/**
 * @author Marko Voß
 *
 */
public enum QuestionType {

  CHOICE,
  BOOL,
  RANGE,
  TEXT,
  CHECKLIST;

  public static QuestionType valueOf(final Class<? extends QuestionDto> clazz) {

    if (BooleanQuestionDto.class.isAssignableFrom(clazz))
      return BOOL;
    if (ChecklistQuestionDto.class.isAssignableFrom(clazz))
      return CHECKLIST;
    if (ChoiceQuestionDto.class.isAssignableFrom(clazz))
      return CHOICE;
    if (RangeQuestionDto.class.isAssignableFrom(clazz))
      return RANGE;
    if (TextQuestionDto.class.isAssignableFrom(clazz))
      return TEXT;

    return null;
  }
}
