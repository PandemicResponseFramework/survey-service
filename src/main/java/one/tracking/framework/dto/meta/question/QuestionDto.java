/**
 *
 */
package one.tracking.framework.dto.meta.question;

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
    @JsonSubTypes.Type(value = TitleQuestionDto.class)
})
@ApiModel(discriminator = "type", subTypes = {
    BooleanQuestionDto.class,
    ChoiceQuestionDto.class,
    RangeQuestionDto.class,
    TextQuestionDto.class,
    TitleQuestionDto.class})
public abstract class QuestionDto {

  private Long id;

  private String question;

  private QuestionType type;

  private Integer order;
}
