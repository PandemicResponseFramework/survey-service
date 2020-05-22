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
  public boolean lock(final String task, final int lockTimeout) {

    final Optional<SchedulerLock> lockOp = this.schedulerLockRepository.findByTaskName(task);

    if (lockOp.isEmpty()) {

      LOG.debug("Creating lock for task: {}", task);

      this.schedulerLockRepository.save(SchedulerLock.builder()
          .taskName(task)
          .timeout(lockTimeout)
          .build());
      return true;

    } else {

      final SchedulerLock lock = lockOp.get();
      // If lock exists but timed out, create a new lock
      // This will happen, if the task has been executed previously
      if (lock.getCreatedAt().plusSeconds(lock.getTimeout()).isAfter(Instant.now())) {

        LOG.debug("Updating lock for task: {}", task);

        this.schedulerLockRepository.save(lock.toBuilder()
            .createdAt(Instant.now())
            .timeout(lockTimeout)
            .build());
        return true;

      }

      return false;
    }
  }

  @Transactional
  public void free(final String task) {

    this.schedulerLockRepository.deleteByTaskName(task);
  }
}
