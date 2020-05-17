/**
 *
 */
package one.tracking.framework.repo;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.meta.Survey;
import one.tracking.framework.entity.meta.question.Question;

/**
 * @author Marko Voß
 *
 */
public interface SurveyRepository extends CrudRepository<Survey, Long> {

  Optional<Survey> findByNameId(String nameId);

  Optional<Survey> findByNameIdAndQuestionsIn(String nameId, Set<Question> questions);
}
