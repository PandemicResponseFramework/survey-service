/**
 *
 */
package one.tracking.framework.entity.meta;

import java.time.Instant;
import java.time.OffsetDateTime;
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

  @Column(nullable = false, updatable = false)
  private Instant timestampCreate;

  @Column(nullable = false, updatable = false)
  private Integer timestampCreateOffset;

  @OneToMany(fetch = FetchType.EAGER)
  private List<Question> questions;

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      final OffsetDateTime timestamp = OffsetDateTime.now();
      setTimestampCreate(timestamp.toInstant());
      setTimestampCreateOffset(timestamp.getOffset().getTotalSeconds());
    }
  }

}
