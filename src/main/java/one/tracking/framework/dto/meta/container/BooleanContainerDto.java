/**
 *
 */
package one.tracking.framework.dto.meta.container;

import javax.validation.constraints.NotNull;
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
@Schema
public class BooleanContainerDto extends ContainerDto {

  @NotNull
  private Boolean boolDependsOn;

}
