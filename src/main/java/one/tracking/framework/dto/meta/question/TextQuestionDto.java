/**
 *
 */
package one.tracking.framework.dto.meta.question;

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
public class TextQuestionDto extends QuestionDto {

  private boolean multiline;

  private DefaultContainerDto container;

}
