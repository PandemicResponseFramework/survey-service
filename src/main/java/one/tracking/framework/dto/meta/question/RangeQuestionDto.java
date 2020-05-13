/**
 *
 */
package one.tracking.framework.dto.meta.question;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.dto.meta.container.DefaultContainerDto;

/**
 * @author Marko Voß
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@ApiModel(parent = QuestionDto.class)
public class RangeQuestionDto extends QuestionDto {

  @NotNull
  private Integer minValue;

  @NotNull
  private Integer maxValue;

  @Size(max = 64)
  private String minText;

  @Size(max = 64)
  private String maxText;

  private Integer defaultValue;

  @Valid
  private DefaultContainerDto container;

}
