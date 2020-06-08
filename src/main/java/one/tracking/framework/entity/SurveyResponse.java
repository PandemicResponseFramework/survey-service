/**
 *
 */
package one.tracking.framework.entity;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.SqlResultSetMapping;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
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
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "survey_instance_id", "question_id", "version"})
    },
    indexes = {
        @Index(name = "IDX_SURVEY_RESPONSE_USER_ID", columnList = "user_id, survey_instance_id, version")
    })
@NamedQueries({
    @NamedQuery(name = "SurveyResponse.findBySurveyInstanceIdAndUserId",
        query = "SELECT COUNT(sr) FROM SurveyResponse sr WHERE sr.surveyInstance.id = ?1 AND sr.user.id = ?2"),
    @NamedQuery(name = "SurveyResponse.findBySurveyInstanceIdAndUserIdAndMaxVersion", query = "SELECT s "
        + "FROM SurveyResponse s "
        + "WHERE s.user.id = ?2"
        + "  AND s.surveyInstance.id = ?1"
        + "  AND s.version = ("
        + "    SELECT MAX(x.version) "
        + "    FROM SurveyResponse x "
        + "    WHERE x.user = s.user "
        + "      AND x.surveyInstance = s.surveyInstance "
        + "      AND x.question = s.question"
        + "  )")
})
@NamedNativeQueries({
    @NamedNativeQuery(name = "SurveyResponse.nativeFindBySurveyInstanceIdAndUserIdIn",
        resultSetMapping = "StringMapping",
        query = "SELECT DISTINCT sr.user_id FROM survey_response sr "
            + "WHERE sr.survey_instance_id = ?1 AND sr.user_id IN (?2)")
})
@SqlResultSetMapping(
    name = "StringMapping",
    classes = {
        @ConstructorResult(
            targetClass = String.class,
            columns = {
                @ColumnResult(name = "user_id")
            })
    })
public class SurveyResponse {

  @Id
  @GeneratedValue
  private Long id;

  /*
   * Not using @Version here as for each version a new entry must exist.
   */
  @Column(nullable = false, updatable = false)
  private Integer version;

  @ManyToOne(optional = false)
  private User user;

  @ManyToOne(optional = false)
  private SurveyInstance surveyInstance;

  @ManyToOne(optional = false)
  private Question question;

  @Column(nullable = false)
  private boolean skipped;

  @Column(nullable = false)
  private boolean valid;

  @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
  @JoinTable(name = "survey_response_answers",
      joinColumns = @JoinColumn(name = "survey_response_id", referencedColumnName = "id"),
      inverseJoinColumns = @JoinColumn(name = "answer_id", referencedColumnName = "id"))
  private List<Answer> answers;

  @Column(nullable = true)
  private Boolean boolAnswer;

  @Column(nullable = true, length = DataConstants.TEXT_ANSWER_MAX_LENGTH)
  private String textAnswer;

  @Column(nullable = true)
  private Integer numberAnswer;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  public SurveyResponseBuilder newVersion() {
    return toBuilder()
        .id(null)
        .createdAt(null)
        .answers(null)
        .boolAnswer(null)
        .textAnswer(null)
        .numberAnswer(null)
        .version(this.version == null ? null : this.version + 1);

  }

  public String toLocalString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("SurveyResponse [id=").append(this.id)
        .append(", version=").append(this.version)
        .append(", user=").append(this.user)
        .append(", skipped=").append(this.skipped)
        .append(", valid=").append(this.valid)
        .append(", answers=").append(this.answers.stream().map(m -> m.getId()).collect(Collectors.toList()))
        .append(", boolAnswer=").append(this.boolAnswer)
        .append(", textAnswer=").append(this.textAnswer)
        .append(", numberAnswer=").append(this.numberAnswer)
        .append(", createdAt=").append(this.createdAt)
        .append("]");
    return builder.toString();
  }



  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());

      if (this.version == null)
        setVersion(0);
    }
  }

}
