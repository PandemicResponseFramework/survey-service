/**
 *
 */
package one.tracking.framework.repo;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.meta.Survey;

/**
 * @author Marko Voß
 *
 */
public interface SurveyRepository extends CrudRepository<Survey, Long> {

  Optional<Survey> findByNameId(String nameId);
}
