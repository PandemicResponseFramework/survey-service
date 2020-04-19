/**
 *
 */
package one.tracking.framework.dto.meta;

import lombok.Builder;
import lombok.Data;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
public class AnswerDto {

  private Long id;

  private String value;

}
