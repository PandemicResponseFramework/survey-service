/**
 *
 */
package one.tracking.framework.dto;

import javax.validation.constraints.NotBlank;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
@ApiModel
public class AuthNTokenResponseDto {

  @NotBlank
  private String token;
}
