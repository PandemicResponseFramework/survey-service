/**
 *
 */
package one.tracking.framework.dto;

import io.swagger.annotations.ApiModel;
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
@AllArgsConstructor
@NoArgsConstructor
@ApiModel
public class SurveyResponseConflictDto {

  private SurveyResponseConflictType conflictType;
}
