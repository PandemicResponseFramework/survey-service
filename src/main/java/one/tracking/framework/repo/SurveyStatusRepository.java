/**
 *
 */
package one.tracking.framework.repo;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.SurveyInstance;
import one.tracking.framework.entity.SurveyStatus;
import one.tracking.framework.entity.User;

/**
 * @author Marko Voß
 *
 */
public interface SurveyStatusRepository extends CrudRepository<SurveyStatus, Long> {

  Optional<SurveyStatus> findByUserAndSurveyInstance(User user, SurveyInstance instance);
}
