/**
 *
 */
package one.tracking.framework.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.tracking.framework.domain.SurveyStatusType;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema
public class SurveyStatusDto {

  @NotBlank
  private String nameId;

  @NotBlank
  @Size(max = 64)
  private String title;

  @Size(max = 256)
  private String description;

  @NotNull
  private Integer countQuestions;

  @NotNull
  private SurveyStatusType status;

  private Long nextQuestionId;

  @NotBlank
  private String token;

  private Long startTime;

  private Long endTime;

  private String dependsOn;
}
