/**
 *
 */
package one.tracking.framework.component;

import java.time.Instant;
import java.util.Optional;
import javax.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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

  @Value("${app.timeout.task.lock}")
  private Integer timeoutLock;

  @Transactional
  public boolean lock(final String task) {

    final Optional<SchedulerLock> lockOp = this.schedulerLockRepository.findByTaskName(task);

    if (lockOp.isEmpty()) {

      LOG.debug("Creating lock for task: {}", task);

      this.schedulerLockRepository.save(SchedulerLock.builder()
          .taskName(task)
          .timeout(this.timeoutLock)
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
            .timeout(this.timeoutLock)
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
