/**
 *
 */
package one.tracking.framework.dto;

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
public class StepCountDto {

  private Integer count;
  private Long startTime;
  private Long endTime;
}
