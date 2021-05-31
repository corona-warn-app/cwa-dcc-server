package app.coronawarn.dcc.repository;

import app.coronawarn.dcc.domain.DccRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DccRegistrationRepository extends JpaRepository<DccRegistration, Long> {

  Optional<DccRegistration> findByRegistrationToken(String registrationToken);
}
