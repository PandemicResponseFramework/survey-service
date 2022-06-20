/**
 *
 */
package one.tracking.framework.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema
public class StepCountDto {

  @Min(0)
  private Integer count;

  @NotNull
  @Min(0)
  private Long startTime;

  @NotNull
  @Min(0)
  private Long endTime;
}
