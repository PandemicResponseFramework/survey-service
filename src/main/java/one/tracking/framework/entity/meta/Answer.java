/**
 *
 */
package one.tracking.framework.entity.meta;

import java.time.Instant;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import one.tracking.framework.entity.SurveyResponse;

/**
 * TODO: ranking
 *
 * @author Marko Voß
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@Entity
public class Answer {

  @Id
  @GeneratedValue
  private Long id;

  @Column(length = 64)
  private String value;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(name = "survey_response_answers",
      joinColumns = @JoinColumn(name = "answer_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "survey_response_id", referencedColumnName = "id"))
  private List<SurveyResponse> surveyResponses;

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final Answer other = (Answer) obj;
    if (this.id != other.id)
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (this.id ^ (this.id >>> 32));
    return result;
  }
}
