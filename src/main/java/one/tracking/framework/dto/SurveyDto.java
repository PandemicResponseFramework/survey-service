/**
 *
 */
package one.tracking.framework.dto;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.annotations.ApiModel;
import lombok.Builder;
import lombok.Data;
import one.tracking.framework.entity.meta.Survey;

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

  public static SurveyDto fromEntity(final Survey entity) {

    return SurveyDto.builder()
        .id(entity.getId())
        .nameId(entity.getNameId())
        .version(entity.getVersion())
        .questions(entity.getQuestions().stream().map(QuestionDto::fromEntity).collect(Collectors.toList()))
        .build();
  }
}
