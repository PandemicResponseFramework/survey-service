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
public class TitleQuestionDto extends QuestionDto {

  @NotNull
  private DefaultContainerDto container;
}
