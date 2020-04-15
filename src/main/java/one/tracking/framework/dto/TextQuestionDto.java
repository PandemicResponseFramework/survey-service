/**
 *
 */
package one.tracking.framework.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.TextQuestion;

/**
 * @author Marko Voß
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ApiModel
public class TextQuestionDto extends QuestionDto {

  private boolean multiline;

  public static final TextQuestionDto fromEntity(final TextQuestion entity) {
    return TextQuestionDto.builder()
        .id(entity.getId())
        .order(entity.getRanking())
        .question(entity.getQuestion())
        .multiline(entity.isMultiline())
        .type(QuestionType.valueOf(entity.getType()))
        .build();
  }
}
