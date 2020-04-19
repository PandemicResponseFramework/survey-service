/**
 *
 */
package one.tracking.framework.dto.meta;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Builder;
import lombok.Data;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
public class AnswerDto {

  @NotNull
  private Long id;

  @NotEmpty
  @Size(max = 256)
  private String value;

}
