/**
 *
 */
package one.tracking.framework.entity.meta.question;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.container.DefaultContainer;

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
public class TextQuestion extends Question implements IContainerQuestion {

  @Column(nullable = false)
  private boolean multiline;

  @Column(nullable = false)
  private int length;

  @OneToOne
  private DefaultContainer container;

  @Override
  public boolean hasContainer() {
    return this.container != null;
  }

  @Override
  @PrePersist
  void onPrePersist() {
    super.onPrePersist();
  }
}
