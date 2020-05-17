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
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import org.hibernate.annotations.Formula;
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
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "CONTAINER_TYPE", discriminatorType = DiscriminatorType.STRING, length = 9)
@Entity
public class Container {

  @Id
  @GeneratedValue
  private Long id;

  @OneToMany
  @OrderBy("ranking ASC")
  private List<Question> questions;

  @Formula("CONTAINER_TYPE")
  private String type;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());
    }
  }
}
