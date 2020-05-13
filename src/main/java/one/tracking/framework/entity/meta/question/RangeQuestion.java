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
@DiscriminatorValue("RANGE")
public class RangeQuestion extends Question {

  @Column(nullable = false)
  private Integer minValue;

  @Column(nullable = true, length = 32)
  private String minText;

  @Column(nullable = false)
  private Integer maxValue;

  @Column(nullable = true, length = 32)
  private String maxText;

  @Column(nullable = true)
  private Integer defaultValue;

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

    // Only allow a default value, which is within the range of minValue and maxValue
    if (this.defaultValue != null) {
      if (this.defaultValue < this.minValue)
        this.defaultValue = null;
      if (this.defaultValue > this.maxValue)
        this.defaultValue = null;
    }
  }
}
