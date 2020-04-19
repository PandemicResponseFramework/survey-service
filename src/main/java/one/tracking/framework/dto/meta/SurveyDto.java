/**
 *
 */
package one.tracking.framework.dto.meta;

import java.util.List;
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

  private Long id;

  private List<QuestionDto> questions;

  private String nameId;

  private Integer version;

}
