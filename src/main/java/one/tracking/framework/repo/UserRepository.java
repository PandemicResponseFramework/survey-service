/**
 *
 */
package one.tracking.framework.repo;

import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.User;

/**
 * @author Marko Voß
 *
 */
public interface UserRepository extends CrudRepository<User, String> {

}
