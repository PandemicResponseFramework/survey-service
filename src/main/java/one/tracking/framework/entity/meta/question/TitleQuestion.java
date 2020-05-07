/**
 *
 */
package one.tracking.framework.entity.meta.question;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.container.DefaultContainer;

/**
 * A {@link TitleQuestion} is a question, that does not have an answer on its own but introduces
 * required sub-questions. This question type is more like a title to sub-questions. Therefore it
 * acts like a container.
 *
 * @deprecated
 * @author Marko Voß
 *
 */
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("TITLE")
@Deprecated
public class TitleQuestion extends Question {

  @OneToOne(optional = false)
  private DefaultContainer container;

  @Override
  @PrePersist
  void onPrePersist() {
    super.onPrePersist();
  }
}
