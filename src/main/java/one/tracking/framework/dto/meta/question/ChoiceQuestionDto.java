/**
 *
 */
package one.tracking.framework.dto.meta.question;

import java.util.List;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@ApiModel
public class ChoiceQuestionDto extends QuestionDto {

  private List<AnswerDto> answers;

  private Long defaultAnswer;

  private boolean multiple;

  private ChoiceContainerDto container;
}
