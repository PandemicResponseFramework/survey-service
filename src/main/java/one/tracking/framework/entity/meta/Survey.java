/**
 *
 */
package one.tracking.framework.entity.meta;

import java.time.Instant;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
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
@Builder(toBuilder = true)
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

  /*
   * No using @Version here as for each version a new entry must exist.
   */
  @Column(nullable = false, updatable = false)
  private Integer version;

  @Column(length = 32, nullable = false, updatable = false)
  private String nameId;

  @Column(length = 32, nullable = false)
  private String title;

  @Column(length = 256, nullable = true)
  private String description;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private IntervalType intervalType;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private ReleaseStatusType releaseStatus;

  @Column(nullable = true)
  private Integer intervalLength;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @OneToMany(fetch = FetchType.LAZY)
  @OrderBy("ranking ASC")
  private List<Question> questions;

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());

      if (this.version == null) {
        setVersion(0);
      }
    }
  }

}
