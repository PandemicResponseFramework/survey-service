/**
 *
 */
package one.tracking.framework.dto.meta.question;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.dto.meta.AnswerDto;
import one.tracking.framework.dto.meta.container.ChoiceContainerDto;

/**
 * @author Marko Voß
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@ApiModel(parent = QuestionDto.class)
public class ChoiceQuestionDto extends QuestionDto {

  @NotEmpty
  private List<@Valid AnswerDto> answers;

  private Long defaultAnswer;

  private boolean multiple;

  @Valid
  private ChoiceContainerDto container;

  @Override
  public List<QuestionDto> getSubQuestions() {
    return this.container == null ? null : this.container.getSubQuestions();
  }
}
