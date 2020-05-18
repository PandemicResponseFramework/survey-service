/**
 *
 */
package one.tracking.framework.dto.meta.question;

import java.util.List;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(discriminatorProperty = "type", discriminatorMapping = {
    @DiscriminatorMapping(value = "BOOL", schema = BooleanQuestionDto.class),
    @DiscriminatorMapping(value = "CHOICE", schema = ChoiceQuestionDto.class),
    @DiscriminatorMapping(value = "RANGE", schema = RangeQuestionDto.class),
    @DiscriminatorMapping(value = "TEXT", schema = TextQuestionDto.class),
    @DiscriminatorMapping(value = "CHECKLIST", schema = ChecklistQuestionDto.class),
    @DiscriminatorMapping(value = "CHECKLIST_ENTRY", schema = ChecklistEntryDto.class)
})
public abstract class QuestionDto {

  @NotNull
  private Long id;

  @NotEmpty
  @Size(max = 256)
  private String question;

  @NotNull
  @Min(0)
  private Integer order;

  @JsonIgnore
  public List<QuestionDto> getSubQuestions() {
    return null;
  }
}
