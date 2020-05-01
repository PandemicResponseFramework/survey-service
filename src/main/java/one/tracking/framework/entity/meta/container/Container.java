/**
 *
 */
package one.tracking.framework.entity.meta.container;

import java.time.Instant;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.question.Question;

/**
 * @author Marko Voß
 *
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "QUESTION_TYPE", discriminatorType = DiscriminatorType.STRING, length = 8)
@Entity
public class Container {

  @Id
  @GeneratedValue
  private Long id;

  @OneToMany
  private List<Question> subQuestions;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());
    }
  }
}
