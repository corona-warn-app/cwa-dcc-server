package app.coronawarn.dcc.repository;

import app.coronawarn.dcc.domain.LabIdClaim;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Transactional
public interface LabIdClaimRepository extends JpaRepository<LabIdClaim, Long> {

  int countByPartnerId(String partnerId);

  Optional<LabIdClaim> findByLabId(String labId);

  @Modifying
  @Query("UPDATE LabIdClaim l SET l.lastUsed = current_timestamp WHERE l = :claim")
  void updateLastUsed(@Param("claim") LabIdClaim claim);

  @Modifying
  @Query("DELETE FROM LabIdClaim l WHERE l.lastUsed < :timestamp")
  int deleteClaimsOlderThan(@Param("timestamp") LocalDateTime timestamp);

}
