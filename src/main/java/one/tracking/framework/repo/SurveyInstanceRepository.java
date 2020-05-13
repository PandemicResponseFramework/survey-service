/**
 *
 */
package one.tracking.framework.repo;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.SurveyInstance;
import one.tracking.framework.entity.meta.Survey;

/**
 * @author Marko Voß
 *
 */
public interface SurveyInstanceRepository extends CrudRepository<SurveyInstance, Long> {

  Optional<SurveyInstance> findBySurveyAndToken(Survey survey, String token);

  Optional<SurveyInstance> findBySurveyAndStartTimeAndEndTime(Survey survey, Instant start, Instant end);

}
