/**
 *
 */
package one.tracking.framework.entity.meta.question;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

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
  private Integer defaultAnswer;

  @Override
  @PrePersist
  void onPrePersist() {

    super.onPrePersist();

    // Only allow a default value, which is within the range of minValue and maxValue
    if (this.defaultAnswer != null) {
      if (this.defaultAnswer < this.minValue)
        this.defaultAnswer = null;
      if (this.defaultAnswer > this.maxValue)
        this.defaultAnswer = null;
    }
  }
}
