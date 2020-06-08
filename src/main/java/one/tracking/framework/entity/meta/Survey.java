/**
 *
 */
package one.tracking.framework.entity.meta;

import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.container.Container;

/**
 * @author Marko Voß
 *
 */
@Data
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(uniqueConstraints = {
    @UniqueConstraint(columnNames = {"nameId", "version"})
})
@NamedQueries({
    @NamedQuery(name = "Survey.findByNameIdAndReleaseStatusAndReminderTypeNotAndIntervalTypeNot",
        query = "SELECT s FROM Survey s "
            + "WHERE s.nameId = ?1 AND s.releaseStatus = ?2 AND s.reminderType <> ?3 AND s.intervalType <> ?4 "
            + "ORDER BY s.version DESC")
})
@DiscriminatorValue("SURVEY")
public class Survey extends Container {

  /*
   * Not using @Version here as for each version a new entry must exist.
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

  @Column(nullable = true)
  private Instant intervalStart;

  @Column(nullable = true)
  private Integer intervalValue;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private ReminderType reminderType;

  @Column(nullable = true)
  private Integer reminderValue;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private ReleaseStatusType releaseStatus;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @ManyToOne(optional = true, fetch = FetchType.LAZY)
  private Survey dependsOn;

  @Override
  @PrePersist
  protected void onPrePersist() {

    if (getId() == null) {
      setCreatedAt(Instant.now());

      if (this.version == null) {
        setVersion(0);
      }
    }
    // Parent question of survey must be null
    setParent(null);
  }

}
