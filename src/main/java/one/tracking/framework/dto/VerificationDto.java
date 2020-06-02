/**
 *
 */
package one.tracking.framework.dto;

import static one.tracking.framework.entity.DataConstants.TOKEN_CONFIRM_LENGTH;
import static one.tracking.framework.entity.DataConstants.TOKEN_VERIFY_LENGTH;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
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
public class VerificationDto {

  @NotBlank
  @Size(max = TOKEN_VERIFY_LENGTH)
  private String verificationToken;

  @Size(max = TOKEN_CONFIRM_LENGTH)
  private String confirmationToken;

}
