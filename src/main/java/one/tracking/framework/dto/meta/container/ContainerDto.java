/**
 *
 */
package one.tracking.framework.dto.meta.container;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.dto.meta.question.QuestionDto;

/**
 * @author Marko Voß
 *
 */
@Data
@SuperBuilder
@JsonSubTypes({
    @JsonSubTypes.Type(value = BooleanContainerDto.class),
    @JsonSubTypes.Type(value = ChoiceContainerDto.class),
    @JsonSubTypes.Type(value = DefaultContainerDto.class),
})
// @ApiModel(discriminator = "type", subTypes = {
// BooleanContainerDto.class,
// ChoiceContainerDto.class,
// DefaultContainerDto.class})
public abstract class ContainerDto {

  @NotEmpty
  @Valid
  private List<QuestionDto> subQuestions;

}
