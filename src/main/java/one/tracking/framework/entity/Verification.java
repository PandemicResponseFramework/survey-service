/**
 *
 */
package one.tracking.framework.entity;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.PrePersist;
import javax.persistence.Table;
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
@Table(indexes = {
    @Index(name = "IDX_EMAIL", columnList = "email"),
    @Index(name = "IDX_HASH", columnList = "hash"),
})
public class Verification {

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false, length = 256, unique = true)
  private String email;

  @Column(nullable = false, length = 256, unique = true)
  private String hash;

  @Column(nullable = false)
  private boolean verified;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = true, updatable = true)
  private Instant updatedAt;

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());
    }
  }
}
