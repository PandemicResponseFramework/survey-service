/**
 *
 */
package one.tracking.framework.component;

import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import one.tracking.framework.config.TimeoutConfig;
import one.tracking.framework.entity.SchedulerLock;
import one.tracking.framework.repo.SchedulerLockRepository;

/**
 * @author Marko Voß
 *
 */
@Component
public class LockerComponent {

  private static final Logger LOG = LoggerFactory.getLogger(LockerComponent.class);

  @Autowired
  private SchedulerLockRepository schedulerLockRepository;

  @Autowired
  private TimeoutConfig timeoutConfig;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean lock(final String task) throws InterruptedException {

    final Optional<SchedulerLock> lockOp = this.schedulerLockRepository.findByTaskName(task);

    if (lockOp.isEmpty()) {

      LOG.debug("Creating lock for task: {}", task);

      this.schedulerLockRepository.save(SchedulerLock.builder()
          .taskName(task)
          .timeout((int) this.timeoutConfig.getTaskLock().toSeconds())
          .build());
      return true;

    } else {

      final SchedulerLock lock = lockOp.get();
      // If lock exists but timed out, create a new lock
      // This will happen, if the task has been executed previously
      if (Instant.now().isAfter(lock.getCreatedAt().plusSeconds(lock.getTimeout()))) {

        LOG.debug("Updating lock for task: {}", task);

        this.schedulerLockRepository.save(lock.toBuilder()
            .createdAt(Instant.now())
            .timeout((int) this.timeoutConfig.getTaskLock().toSeconds())
            .build());
        return true;

      }

      return false;
    }
  }

  @Transactional
  public void free(final String task) {

    LOG.debug("Deleting lock for task: {}", task);
    this.schedulerLockRepository.deleteByTaskName(task);
  }
}
