/**
 *
 */
package one.tracking.framework.dto;

import java.time.Instant;
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

  private Long lastQuestionId;

  @NotBlank
  private String token;

  private Instant startTime;

  private Instant endTime;
}
