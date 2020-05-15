/**
 *
 */
package one.tracking.framework.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.meta.ReleaseStatusType;
import one.tracking.framework.entity.meta.Survey;

/**
 * @author Marko Voß
 *
 */
public interface SurveyRepository extends CrudRepository<Survey, Long> {

  List<Survey> findByNameId(String nameId);

  Optional<Survey> findTopByNameIdOrderByVersionDesc(String nameId);

  Optional<Survey> findTopByNameIdAndReleaseStatusOrderByVersionDesc(String nameId, ReleaseStatusType status);

  List<Survey> findByNameIdOrderByVersionDesc(String nameId);

  List<Survey> findByNameIdAndQuestionIdOrderByVersionDesc(String nameId, Long questionId);
}
