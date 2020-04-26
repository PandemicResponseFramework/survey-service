/**
 *
 */
package one.tracking.framework.dto.meta.container;

import com.nimbusds.jose.util.Container;
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
@ApiModel(parent = Container.class)
public class BooleanContainerDto extends ContainerDto {

  private Boolean boolDependsOn;

}
