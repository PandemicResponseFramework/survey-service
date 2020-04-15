/**
 *
 */
package one.tracking.framework.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.BooleanQuestion;

/**
 * @author Marko Voß
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ApiModel
public class BooleanQuestionDto extends QuestionDto {

  private Boolean defaultValue;

  public static final BooleanQuestionDto fromEntity(final BooleanQuestion entity) {
    return BooleanQuestionDto.builder()
        .id(entity.getId())
        .order(entity.getRanking())
        .question(entity.getQuestion())
        .defaultValue(entity.getDefaultValue())
        .type(QuestionType.valueOf(entity.getType()))
        .build();
  }
}
