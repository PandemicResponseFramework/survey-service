/**
 *
 */
package one.tracking.framework.dto;

import static one.tracking.framework.entity.DataConstants.TOKEN_DEVICE_MAX_LENGTH;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
public class DeviceTokenDto {

  @NotBlank
  @Size(max = TOKEN_DEVICE_MAX_LENGTH)
  private String deviceToken;
}
