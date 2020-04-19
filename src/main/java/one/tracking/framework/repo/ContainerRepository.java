/**
 *
 */
package one.tracking.framework.repo;

import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.meta.container.Container;

/**
 * @author Marko Voß
 *
 */
public interface ContainerRepository extends CrudRepository<Container, Long> {

}
