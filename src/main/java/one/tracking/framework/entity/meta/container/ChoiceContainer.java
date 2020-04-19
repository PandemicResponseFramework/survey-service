/**
 *
 */
package one.tracking.framework.entity.meta.container;

import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import one.tracking.framework.entity.meta.Answer;

/**
 * @author Marko Voß
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("CHOICE")
public class ChoiceContainer extends Container {

  @OneToMany
  private List<Answer> dependsOn;

}
