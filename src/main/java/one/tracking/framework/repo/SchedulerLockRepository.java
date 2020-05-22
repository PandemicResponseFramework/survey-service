/**
 *
 */
package one.tracking.framework.repo;

import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.SchedulerLock;

/**
 * @author Marko Voß
 *
 */
public interface SchedulerLockRepository extends CrudRepository<SchedulerLock, Long> {

  @Lock(LockModeType.PESSIMISTIC_READ)
  Optional<SchedulerLock> findByTaskName(String name);

  void deleteByTaskName(String name);
}
