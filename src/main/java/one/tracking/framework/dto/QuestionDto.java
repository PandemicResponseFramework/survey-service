/**
 *
 */
package one.tracking.framework.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.BooleanQuestion;
import one.tracking.framework.entity.meta.ChoiceQuestion;
import one.tracking.framework.entity.meta.Question;
import one.tracking.framework.entity.meta.RangeQuestion;
import one.tracking.framework.entity.meta.TextQuestion;

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
    @JsonSubTypes.Type(value = TextQuestionDto.class)
})
@ApiModel(discriminator = "type", subTypes = {
    BooleanQuestionDto.class,
    ChoiceQuestionDto.class,
    RangeQuestionDto.class,
    TextQuestionDto.class})
public abstract class QuestionDto {

  private Long id;

  private String question;

  private QuestionType type;

  private Integer order;

  public static QuestionDto fromEntity(final Question entity) {

    if (entity instanceof BooleanQuestion)
      return BooleanQuestionDto.fromEntity((BooleanQuestion) entity);
    if (entity instanceof ChoiceQuestion)
      return ChoiceQuestionDto.fromEntity((ChoiceQuestion) entity);
    if (entity instanceof RangeQuestion)
      return RangeQuestionDto.fromEntity((RangeQuestion) entity);
    if (entity instanceof TextQuestion)
      return TextQuestionDto.fromEntity((TextQuestion) entity);

    return null;
  }
}
