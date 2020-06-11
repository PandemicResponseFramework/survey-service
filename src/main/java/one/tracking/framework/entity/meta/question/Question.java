/**
 *
 */
package one.tracking.framework.entity.meta.question;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import javax.persistence.Transient;
import org.hibernate.annotations.Formula;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.container.Container;

/**
 * @author Marko Voß
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "QUESTION_TYPE", discriminatorType = DiscriminatorType.STRING, length = 15)
@Entity
public class Question {

  @Id
  @GeneratedValue
  private Long id;

  @Column(length = 256, nullable = false)
  private String question;

  @Column(nullable = false)
  private int ranking;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @Formula("QUESTION_TYPE")
  private String typeString;

  @Transient
  @Setter(AccessLevel.NONE)
  private QuestionType type;

  @Column(nullable = false)
  private boolean optional;

  /**
   * Convenience method to check if this {@link Question} owns a {@link Container}.
   *
   * @return <code>true</code> if and only if this {@link Question} owns a {@link Container},
   *         <code>false</code> otherwise
   */
  public boolean hasContainer() {
    return false;
  }

  /**
   * Convenience method to get the {@link Container} of this {@link Question}.
   *
   * @return the {@link Container} owned by this {@link Question}
   */
  public Container getContainer() {
    return null;
  }

  /**
   * Convenience method to get the {@link List} of {@link Question}s of the {@link Container} owned by
   * this {@link Question}.
   *
   * @return the {@link List} of {@link Question}s of the {@link Container} owned by this
   *         {@link Question}
   */
  public List<Question> getSubQuestions() {
    return Collections.emptyList();
  }

  @PostLoad
  void onPostLoad() {
    this.type = QuestionType.valueOf(this.typeString);
  }

  @PrePersist
  void onPrePersist() {
    if (this.id == null) {
      setCreatedAt(Instant.now());
    }
  }
}
