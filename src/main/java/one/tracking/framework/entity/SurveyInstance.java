/**
 *
 */
package one.tracking.framework.entity;

import static one.tracking.framework.entity.DataConstants.TOKEN_SURVEY_LENGTH;
import java.time.Instant;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.tracking.framework.entity.meta.Survey;

/**
 * @author Marko Voß
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"survey_id", "token"})
    },
    indexes = {
        @Index(name = "IDX_TOKEN", columnList = "token"),
    })
public class SurveyInstance {

  @Id
  @GeneratedValue
  private Long id;

  @Column(nullable = false, length = TOKEN_SURVEY_LENGTH)
  private String token;

  @Column(nullable = false)
  private Instant startTime;

  @Column(nullable = false)
  private Instant endTime;

  @ManyToOne(optional = false)
  private Survey survey;
}
