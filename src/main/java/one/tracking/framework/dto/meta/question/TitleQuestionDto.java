/**
 *
 */
package one.tracking.framework.dto.meta.question;

import javax.validation.Valid;
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
@ApiModel(parent = QuestionDto.class)
public class TitleQuestionDto extends QuestionDto {

  @NotNull
  @Valid
  private DefaultContainerDto container;
}
