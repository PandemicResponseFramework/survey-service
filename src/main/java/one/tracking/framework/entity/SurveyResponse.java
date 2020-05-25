/**
 *
 */
package one.tracking.framework.entity;

import java.time.Instant;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.tracking.framework.entity.meta.Answer;
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
    @UniqueConstraint(columnNames = {"user_id", "survey_instance_id", "question_id"})
})
public class SurveyResponse {

  @Id
  @GeneratedValue
  private Long id;

  @Version
  private Integer version;

  @ManyToOne(optional = false)
  private User user;

  @ManyToOne(optional = false)
  private SurveyInstance surveyInstance;

  @ManyToOne(optional = false)
  private Question question;

  @ManyToMany(cascade = CascadeType.ALL)
  private List<Answer> answers;

  @Column(nullable = true)
  private Boolean boolAnswer;

  @Column(nullable = true, length = DataConstants.TEXT_ANSWER_MAX_LENGTH)
  private String textAnswer;

  @Column(nullable = true)
  private Integer numberAnswer;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());
    }
  }
}
