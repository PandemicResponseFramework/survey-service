/**
 *
 */
package one.tracking.framework.dto;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.RangeQuestion;

/**
 * @author Marko Voß
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ApiModel
public class RangeQuestionDto extends QuestionDto {

  private Integer minValue;

  private Integer maxValue;

  private Integer defaultValue;

  public static final RangeQuestionDto fromEntity(final RangeQuestion entity) {
    return RangeQuestionDto.builder()
        .id(entity.getId())
        .order(entity.getRanking())
        .question(entity.getQuestion())
        .defaultValue(entity.getDefaultValue())
        .minValue(entity.getMinValue())
        .maxValue(entity.getMaxValue())
        .type(QuestionType.valueOf(entity.getType()))
        .build();
  }
}
