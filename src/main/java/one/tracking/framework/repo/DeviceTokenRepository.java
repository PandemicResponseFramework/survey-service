/**
 *
 */
package one.tracking.framework.repo;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import one.tracking.framework.entity.DeviceToken;
import one.tracking.framework.entity.User;

/**
 * @author Marko Voß
 *
 */
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

  List<DeviceToken> findByUser(User user);

  Optional<DeviceToken> findByUserAndToken(User user, String token);

  List<DeviceToken> findByUserNotAndToken(User user, String deviceToken);

}
