/**
 *
 */
package one.tracking.framework.entity;

import static one.tracking.framework.entity.DataConstants.TOKEN_DEVICE_MAX_LENGTH;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
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
@Table(
    indexes = {
        @Index(name = "IDX_DEVICE_TOKEN", columnList = "token"),
    },
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"token", "user_id"})
    })
public class DeviceToken {

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false, length = TOKEN_DEVICE_MAX_LENGTH)
  private String token;

  @ManyToOne(optional = false)
  private User user;

  @Column(nullable = false)
  private Instant createdAt;

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());
    }
  }
}
