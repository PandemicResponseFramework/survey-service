/**
 *
 */
package one.tracking.framework.repo;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;
import one.tracking.framework.entity.Verification;

/**
 * @author Marko Voß
 *
 */
public interface VerificationRepository extends CrudRepository<Verification, Long> {

  Optional<Verification> findByHashAndVerified(String hash, boolean verified);

  Optional<Verification> findByEmail(String email);

  boolean existsByHash(String hash);
}
