/**
 *
 */
package one.tracking.framework.dto.meta;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import one.tracking.framework.dto.meta.question.QuestionDto;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
@ApiModel
public class SurveyDto {

  @NotNull
  private Long id;

  @NotEmpty
  @Valid
  private List<QuestionDto> questions;

  @NotBlank
  private String nameId;

  @Size(max = 256)
  private String description;

  @NotNull
  private Integer version;

}
