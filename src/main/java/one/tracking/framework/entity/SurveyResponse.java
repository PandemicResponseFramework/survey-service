/**
 *
 */
package one.tracking.framework.entity;

import java.time.Instant;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.Survey;
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
// @Table(uniqueConstraints = {
// @UniqueConstraint(columnNames = {"user_id", "survey_id", "question_id"})
// })
public class SurveyResponse {

  @Id
  @GeneratedValue
  private Long id;

  @ManyToOne(optional = false)
  private User user;

  @ManyToOne(optional = false)
  private Survey survey;

  @ManyToOne(optional = false)
  private Question question;

  @ManyToMany
  private List<Answer> answers;

  private Integer rangeAnswer;

  @Column(nullable = true)
  private Boolean boolAnswer;

  @Column(nullable = true, length = 512)
  private String textAnswer;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());
    }
  }
}
