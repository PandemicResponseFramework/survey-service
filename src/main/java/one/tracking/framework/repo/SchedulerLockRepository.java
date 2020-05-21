/**
 *
 */
package one.tracking.framework.repo;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.SchedulerLock;

/**
 * @author Marko Voß
 *
 */
public interface SchedulerLockRepository extends CrudRepository<SchedulerLock, Long> {

  Optional<SchedulerLock> findByTaskName(String name);
}
