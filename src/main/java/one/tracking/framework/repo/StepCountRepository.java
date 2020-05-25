/**
 *
 */
package one.tracking.framework.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.health.StepCount;

/**
 * @author Marko Voß
 *
 */
public interface StepCountRepository extends CrudRepository<StepCount, Long> {

  Optional<StepCount> findByUserAndStartTimeAndEndTime(User user, Instant start, Instant end);

  List<StepCount> findByUser(User user);
}
