/**
 *
 */
package one.tracking.framework.entity.meta;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
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
@DiscriminatorValue("TEXT")
public class TextQuestion extends Question {

  @Column(nullable = false)
  private boolean multiline;

  @Override
  @PrePersist
  void onPrePersist() {
    super.onPrePersist();
  }
}
