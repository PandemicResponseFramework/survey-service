/**
 *
 */
package one.tracking.framework.dto;

import lombok.Builder;
import lombok.Data;
import one.tracking.framework.entity.meta.Answer;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
public class AnswerDto {

  private Long id;

  private String value;

  public static final AnswerDto fromEntity(final Answer entity) {
    return AnswerDto.builder()
        .id(entity.getId())
        .value(entity.getValue())
        .build();
  }
}
