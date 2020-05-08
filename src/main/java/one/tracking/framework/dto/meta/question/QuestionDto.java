/**
 *
 */
package one.tracking.framework.dto.meta.question;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.SuperBuilder;

/**
 * @author Marko Voß
 *
 */
@Data
@SuperBuilder
@JsonSubTypes({
    @JsonSubTypes.Type(value = BooleanQuestionDto.class),
    @JsonSubTypes.Type(value = ChoiceQuestionDto.class),
    @JsonSubTypes.Type(value = RangeQuestionDto.class),
    @JsonSubTypes.Type(value = TextQuestionDto.class),
    @JsonSubTypes.Type(value = TitleQuestionDto.class),
    @JsonSubTypes.Type(value = ChecklistQuestionDto.class)
})
@ApiModel(discriminator = "type", subTypes = {
    BooleanQuestionDto.class,
    ChoiceQuestionDto.class,
    RangeQuestionDto.class,
    TextQuestionDto.class,
    TitleQuestionDto.class,
    ChecklistQuestionDto.class})
public abstract class QuestionDto {

  @NotNull
  private Long id;

  @NotEmpty
  @Size(max = 256)
  private String question;

  @NotNull
  private QuestionType type;

  @NotNull
  private Integer order;
}
