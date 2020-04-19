/**
 *
 */
package one.tracking.framework.dto.meta.question;

import javax.validation.constraints.NotNull;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.dto.meta.container.DefaultContainerDto;

/**
 * @author Marko Voß
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ApiModel
public class RangeQuestionDto extends QuestionDto {

  @NotNull
  private Integer minValue;

  @NotNull
  private Integer maxValue;

  private String minText;

  private String maxText;

  private Integer defaultValue;

  private DefaultContainerDto container;

}
