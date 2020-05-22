/**
 *
 */
package one.tracking.framework.repo;

import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.DeviceToken;
import one.tracking.framework.entity.Reminder;
import one.tracking.framework.entity.SurveyInstance;

/**
 * @author Marko Voß
 *
 */
public interface ReminderRepository extends CrudRepository<Reminder, Long> {

  boolean existsByDeviceTokenAndSurveyInstance(DeviceToken token, SurveyInstance instance);

  void deleteBySurveyInstance(SurveyInstance instance);
}
