/**
 *
 */
package one.tracking.framework.dto.meta.container;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.dto.meta.question.QuestionDto;

/**
 * @author Marko Voß
 *
 */
@Data
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = BooleanContainerDto.class, name = "BOOL"),
    @JsonSubTypes.Type(value = ChoiceContainerDto.class, name = "CHOICE"),
    @JsonSubTypes.Type(value = DefaultContainerDto.class, name = "DEFAULT"),
})
@ApiModel(discriminator = "type", subTypes = {
    BooleanContainerDto.class,
    ChoiceContainerDto.class,
    DefaultContainerDto.class})
public abstract class ContainerDto {

  @NotEmpty
  @Valid
  private List<QuestionDto> subQuestions;

}
