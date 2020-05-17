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
    @UniqueConstraint(columnNames = {"survey_instance_id", "user_id"})
})
public class SurveyStatus {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(optional = false)
  private SurveyInstance surveyInstance;

  @ManyToOne(optional = false)
  private User user;

  @ManyToOne(optional = false)
  private Question lastQuestion;

  @ManyToOne(optional = true)
  private Question nextQuestion;
}
