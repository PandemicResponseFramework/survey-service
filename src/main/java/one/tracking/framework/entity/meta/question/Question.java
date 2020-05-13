/**
 *
 */
package one.tracking.framework.entity.meta.question;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PrePersist;
import org.hibernate.annotations.Formula;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author Marko Voß
 *
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "QUESTION_TYPE", discriminatorType = DiscriminatorType.STRING, length = 9)
@Entity
public class Question {

  @Id
  @GeneratedValue
  private Long id;

  @Column(length = 256, nullable = false)
  private String question;

  @Column(nullable = false)
  private int ranking;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Formula("QUESTION_TYPE")
  private String type;

  public boolean hasContainer() {
    return false;
  }

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());
    }
  }
}
