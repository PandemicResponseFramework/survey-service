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
import io.swagger.annotations.ApiModel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TODO: Currently we do not support text answers.
 *
 * @author Marko Voß
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ApiModel
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
