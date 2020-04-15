/**
 *
 */
package one.tracking.framework.dto;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.ChoiceQuestion;

/**
 * @author Marko Voß
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ApiModel
public class ChoiceQuestionDto extends QuestionDto {

  private List<AnswerDto> answers;

  private Long defaultAnswer;

  private boolean multiple;

  public static final ChoiceQuestionDto fromEntity(final ChoiceQuestion entity) {
    return ChoiceQuestionDto.builder()
        .id(entity.getId())
        .order(entity.getRanking())
        .question(entity.getQuestion())
        .defaultAnswer(entity.getDefaultValue() == null ? null : entity.getDefaultValue().getId())
        .answers(entity.getAnswers().stream().map(AnswerDto::fromEntity).collect(Collectors.toList()))
        .multiple(entity.getMultiple())
        .type(QuestionType.valueOf(entity.getType()))
        .build();
  }
}
