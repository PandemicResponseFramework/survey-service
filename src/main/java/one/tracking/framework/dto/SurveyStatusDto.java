/**
 *
 */
package one.tracking.framework.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
public class SurveyStatusDto {

  @NotBlank
  private String nameId;

  @NotNull
  private SurveyStatusType status;

  private Long nextQuestionId;

  @NotBlank
  private String token;

  private Long startTime;

  private Long endTime;
}
