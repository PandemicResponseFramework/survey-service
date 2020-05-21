/**
 *
 */
package one.tracking.framework.repo;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.User;

/**
 * @author Marko Voß
 *
 */
public interface UserRepository extends CrudRepository<User, String> {

  Optional<User> findByUserToken(String token);

  boolean existsByUserToken(String hash);
}
