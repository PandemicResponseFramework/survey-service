/**
 *
 */
package one.tracking.framework.repo;

import java.util.List;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.DeviceToken;
import one.tracking.framework.entity.User;

/**
 * @author Marko Voß
 *
 */
public interface DeviceTokenRepository extends CrudRepository<DeviceToken, Long> {

  List<DeviceToken> findByUser(User user);
}
