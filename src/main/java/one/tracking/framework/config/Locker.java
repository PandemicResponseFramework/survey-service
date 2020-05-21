/**
 *
 */
package one.tracking.framework.config;

import java.time.Instant;
import java.util.Optional;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import one.tracking.framework.entity.SchedulerLock;
import one.tracking.framework.repo.SchedulerLockRepository;

/**
 * @author Marko Voß
 *
 */
@Component
public class Locker {

  private static final Logger LOG = LoggerFactory.getLogger(Locker.class);

  @Autowired
  private SchedulerLockRepository schedulerLockRepository;

  @Transactional
  public void lock(final String task, final int lockTimeout) {

    final Optional<SchedulerLock> lockOp = this.schedulerLockRepository.findByTaskName(task);

    if (lockOp.isEmpty()) {

      LOG.info("CREATING LOCK: {}", task);

      this.schedulerLockRepository.save(SchedulerLock.builder()
          .taskName(task)
          .timeout(lockTimeout)
          .build());

    } else {

      final SchedulerLock lock = lockOp.get();
      // If lock exists but timed out, create a new lock
      // This will happen, if the task has been executed prevously
      if (lock.getCreatedAt().plusSeconds(lock.getTimeout()).isAfter(Instant.now())) {

        this.schedulerLockRepository.save(lock.toBuilder()
            .createdAt(Instant.now())
            .timeout(lockTimeout)
            .build());

      }

    }
  }
}
