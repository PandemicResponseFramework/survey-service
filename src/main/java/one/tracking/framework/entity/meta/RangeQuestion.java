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
@DiscriminatorValue("RANGE")
public class RangeQuestion extends Question {

  @Column(nullable = false)
  private Integer minValue;

  @Column(nullable = false)
  private Integer maxValue;

  @Column(nullable = true)
  private Integer defaultValue;

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
