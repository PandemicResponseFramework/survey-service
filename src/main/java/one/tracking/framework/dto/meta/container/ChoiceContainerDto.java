/**
 *
 */
package one.tracking.framework.dto.meta.container;

import java.util.List;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * @author Marko Voß
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ApiModel
public class ChoiceContainerDto extends ContainerDto {

  private List<Long> dependsOn;
}
