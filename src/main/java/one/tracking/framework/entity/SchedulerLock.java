/**
 *
 */
package one.tracking.framework.entity;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class SchedulerLock {

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false, length = 32, unique = true)
  private String taskName;

  @Column(nullable = false)
  private Integer timeout;

  @Column(nullable = false)
  private Instant createdAt;

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());
    }
  }
}
