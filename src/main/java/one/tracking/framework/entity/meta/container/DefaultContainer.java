/**
 *
 */
package one.tracking.framework.entity.meta.container;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * @author Marko Voß
 *
 */
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("DEFAULT")
public class DefaultContainer extends Container {

}
