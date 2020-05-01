/**
 *
 */
package one.tracking.framework.entity.meta;

import java.time.Instant;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.tracking.framework.entity.meta.question.Question;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"nameId", "version"})
})
public class Survey {

  @Id
  @GeneratedValue
  private Long id;

  @Version
  private Integer version;

  @Column(length = 32, nullable = false, updatable = false)
  private String nameId;

  @Column(length = 256, nullable = true)
  private String description;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @OneToMany(fetch = FetchType.LAZY)
  private List<Question> questions;

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());
    }
  }

}
