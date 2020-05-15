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
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.container.BooleanContainer;

/**
 * @author Marko Voß
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@DiscriminatorValue("BOOL")
public class BooleanQuestion extends Question implements IContainerQuestion {

  @Column(nullable = true)
  private Boolean defaultValue;

  @OneToOne
  private BooleanContainer container;

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
