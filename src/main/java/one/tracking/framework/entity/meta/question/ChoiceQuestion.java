/**
 *
 */
package one.tracking.framework.entity.meta.question;

import java.util.Collections;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.Answer;
import one.tracking.framework.entity.meta.container.ChoiceContainer;

/**
 * @author Marko Voß
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@DiscriminatorValue("CHOICE")
public class ChoiceQuestion extends Question {

  @OneToMany(fetch = FetchType.LAZY)
  private List<Answer> answers;

  @Column(nullable = false)
  private Boolean multiple;

  @OneToOne(fetch = FetchType.LAZY)
  private Answer defaultAnswer;

  @OneToOne
  private ChoiceContainer container;

  @Override
  public boolean hasContainer() {
    return this.container != null;
  }

  @Override
  public List<Question> getSubQuestions() {
    return this.container == null ? Collections.emptyList() : this.container.getQuestions();
  }

  @Override
  @PrePersist
  void onPrePersist() {

    super.onPrePersist();

    // Only allow a default answer, which is part of the available answers
    if (this.answers != null && this.defaultAnswer != null
        && this.answers.stream().noneMatch(p -> p.equals(this.defaultAnswer)))
      this.defaultAnswer = null;
  }
}
