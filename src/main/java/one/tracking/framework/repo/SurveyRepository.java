/**
 *
 */
package one.tracking.framework.repo;

import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.meta.Survey;

/**
 * @author Marko Voß
 *
 */
public interface SurveyRepository extends CrudRepository<Survey, Long> {

}
