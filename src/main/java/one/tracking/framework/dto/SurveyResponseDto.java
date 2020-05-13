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
import lombok.Builder;
import lombok.Data;

/**
 * TODO: Currently we do not support text answers.
 *
 * @author Marko Voß
 *
 */
@Data
@Builder
@ApiModel
public class SurveyResponseDto {

  @NotNull
  private Long questionId;

  private List<Long> answerIds;

  private Boolean boolAnswer;

  private String textAnswer;

  private Integer rangeAnswer;

  private Map<Long, Boolean> checklistAnswer;

  @NotBlank
  @Size(max = TOKEN_SURVEY_LENGTH)
  private String surveyToken;
}
