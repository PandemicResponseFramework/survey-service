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
import one.tracking.framework.dto.meta.container.BooleanContainerDto;

/**
 * @author Marko Voß
 *
 */
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@ApiModel(parent = QuestionDto.class)
public class BooleanQuestionDto extends QuestionDto {

  private Boolean defaultAnswer;

  @Valid
  private BooleanContainerDto container;

  @Override
  public List<QuestionDto> getSubQuestions() {
    return this.container == null ? null : this.container.getSubQuestions();
  }
}
