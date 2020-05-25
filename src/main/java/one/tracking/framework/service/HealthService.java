/**
 *
 */
package one.tracking.framework.service;

import java.time.Instant;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import one.tracking.framework.dto.StepCountDto;
import one.tracking.framework.entity.User;
import one.tracking.framework.entity.health.StepCount;
import one.tracking.framework.repo.StepCountRepository;
import one.tracking.framework.repo.UserRepository;

/**
 * @author Marko Voß
 *
 */
@Service
public class HealthService {

  @Autowired
  private StepCountRepository stepCountRepository;

  @Autowired
  private UserRepository userRepository;

  public void storeStepCount(final String userId, final StepCountDto stepCountDto) {

    final User user = this.userRepository.findById(userId).get();

    final Instant start = Instant.ofEpochMilli(stepCountDto.getStartTime());
    final Instant end = Instant.ofEpochMilli(stepCountDto.getEndTime());

    final Optional<StepCount> entityOp = this.stepCountRepository.findByUserAndStartTimeAndEndTime(user, start, end);

    if (entityOp.isEmpty()) {

      this.stepCountRepository.save(StepCount.builder()
          .user(user)
          .stepCount(stepCountDto.getCount())
          .startTime(start)
          .endTime(end)
          .build());

    } else if (stepCountDto.getCount() > entityOp.get().getStepCount()) {

      final StepCount entity = entityOp.get();
      entity.setStepCount(stepCountDto.getCount());
      entity.setUpdatedAt(Instant.now());
      this.stepCountRepository.save(entity);

    }
  }
}
