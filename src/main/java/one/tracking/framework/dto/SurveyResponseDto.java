/**
 *
 */
package one.tracking.framework.dto;

import static one.tracking.framework.entity.DataConstants.TOKEN_SURVEY_LENGTH;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 * @author Marko Voß
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema
public class SurveyResponseDto {

  @NotNull
  private Long questionId;

  private Boolean skipped;

  private List<@NotNull Long> answerIds;

  private Boolean boolAnswer;

  private String textAnswer;

  private Integer numberAnswer;

  private Map<@NotNull Long, @NotNull Boolean> checklistAnswer;

  @NotBlank
  @Size(max = TOKEN_SURVEY_LENGTH)
  private String surveyToken;
}
