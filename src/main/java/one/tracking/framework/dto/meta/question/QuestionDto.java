/**
 *
 */
package one.tracking.framework.dto.meta.question;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

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
    @Type(value = BooleanQuestionDto.class, name = "BOOL"),
    @Type(value = ChoiceQuestionDto.class, name = "CHOICE"),
    @Type(value = RangeQuestionDto.class, name = "RANGE"),
    @Type(value = TextQuestionDto.class, name = "TEXT"),
    @Type(value = ChecklistQuestionDto.class, name = "CHECKLIST"),
    @Type(value = ChecklistEntryDto.class, name = "CHECKLIST_ENTRY")
})
@ApiModel(discriminator = "type", subTypes = {
    BooleanQuestionDto.class,
    ChoiceQuestionDto.class,
    RangeQuestionDto.class,
    TextQuestionDto.class,
    ChecklistQuestionDto.class,
    ChecklistEntryDto.class})
public abstract class QuestionDto {

  @NotNull
  private Long id;

  @NotEmpty
  @Size(max = 256)
  private String question;

  @NotNull
  @Min(0)
  private Integer order;

}
