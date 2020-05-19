/**
 *
 */
package one.tracking.framework.dto.meta.container;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author Marko Voß
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@ApiModel(parent = ContainerDto.class)
@Schema(allOf = {ContainerDto.class})
public class BooleanContainerDto extends ContainerDto {

  private Boolean boolDependsOn;

}
