/**
 *
 */
package one.tracking.framework.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema
public class SurveyResponseConflictDto {

  private SurveyResponseConflictType conflictType;
}
