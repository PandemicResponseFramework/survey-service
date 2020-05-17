/**
 *
 */
package one.tracking.framework.repo;

import java.util.Optional;
import java.util.Set;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.meta.container.Container;
import one.tracking.framework.entity.meta.question.Question;

/**
 * @author Marko Voß
 *
 */
public interface ContainerRepository extends CrudRepository<Container, Long> {

  Optional<Container> findBySubQuestionsIn(Set<Question> questions);
}
