/**
 *
 */
package one.tracking.framework.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
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
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"device_token_id", "survey_instance_id"})
    })
public class Reminder {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(optional = false)
  private DeviceToken deviceToken;

  @ManyToOne(optional = false)
  private SurveyInstance surveyInstance;
}
