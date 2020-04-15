/**
 *
 */
package one.tracking.framework.repo;

import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.meta.Answer;

/**
 * @author Marko Voß
 *
 */
public interface AnswerRepository extends CrudRepository<Answer, Long> {

}
