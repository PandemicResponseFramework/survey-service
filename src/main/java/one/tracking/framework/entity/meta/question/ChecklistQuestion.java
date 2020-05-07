/**
 *
 */
package one.tracking.framework.entity.meta.question;

import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.validation.ConstraintViolationException;
import javax.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author Marko Voß
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("CHECKLIST")
public class ChecklistQuestion extends Question {

  @NotEmpty
  @OneToMany
  private List<BooleanQuestion> entries;

  @Override
  @PrePersist
  void onPrePersist() {
    super.onPrePersist();

    // Validate that entries do not support sub questions
    if (this.entries.stream().anyMatch(p -> p.getContainer() != null)) {
      throw new ConstraintViolationException("Entries of the ChecklistQuestion type must not contain sub questions.",
          null);
    }
  }
}

