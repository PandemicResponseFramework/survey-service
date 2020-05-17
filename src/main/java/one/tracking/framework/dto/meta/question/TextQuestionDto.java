/**
 *
 */
package one.tracking.framework.dto.meta.question;

import java.util.List;
import javax.validation.Valid;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.dto.meta.container.DefaultContainerDto;

/**
 * @author Marko Voß
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@ApiModel(parent = QuestionDto.class)
public class TextQuestionDto extends QuestionDto {

  private boolean multiline;

  private int length;

  @Valid
  private DefaultContainerDto container;

  @Override
  public List<QuestionDto> getSubQuestions() {
    return this.container == null ? null : this.container.getSubQuestions();
  }
}
