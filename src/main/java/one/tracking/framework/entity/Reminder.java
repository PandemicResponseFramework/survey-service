/**
 *
 */
package one.tracking.framework.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
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
@NamedQueries({
    @NamedQuery(name = "Reminder.findBySurveyInstanceIdAndDeviceTokenId",
        query = "SELECT r FROM Reminder r WHERE r.surveyInstance.id = ?1 AND r.deviceToken.id IN (?2)"),
    @NamedQuery(name = "Reminder.deleteByDeviceTokenId",
        query = "DELETE FROM Reminder r WHERE r.deviceToken.id = ?1")
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
