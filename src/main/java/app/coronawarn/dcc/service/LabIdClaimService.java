package app.coronawarn.dcc.service;

import app.coronawarn.dcc.config.DccApplicationConfig;
import app.coronawarn.dcc.domain.LabIdClaim;
import app.coronawarn.dcc.repository.LabIdClaimRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabIdClaimService {

  private final LabIdClaimRepository labIdClaimRepository;

  private final DccApplicationConfig config;

  /**
   * Checks if Claim already exists. If not creates a new one if quota is not already exceeded.
   *
   * @param partnerId the partner ID
   * @param labId     the lab ID
   * @return true if claiming was successful, false if not.
   */
  public boolean getClaim(String partnerId, String labId) {
    log.debug("Trying to get claim for partnerId {} and labId {}", partnerId, labId);

    Optional<LabIdClaim> claim = labIdClaimRepository.findByLabId(labId);

    if (claim.isPresent() && claim.get().getPartnerId().equals(partnerId)) {
      log.debug("Found Claim for partnerId {} and labId {} with ID {}", partnerId, labId, claim.get().getId());
      labIdClaimRepository.updateLastUsed(claim.get());
      return true;
    } else if (claim.isPresent()) {
      log.debug("Found Claim for labId {} but for partner ID {} != {}", labId, claim.get().getPartnerId(), partnerId);
      return false;
    }

    log.debug("Could not find existing claim for labID {}", labId);

    int claimsOfPartnerID = labIdClaimRepository.countByPartnerId(partnerId);
    if (claimsOfPartnerID >= config.getLabIdClaim().getClaimsPerPartner()) {
      log.info("Partner with ID {} has exceeded labId limit", partnerId);
      return false;
    }

    LabIdClaim newClaim = LabIdClaim.builder()
      .labId(labId)
      .partnerId(partnerId)
      .build();

    labIdClaimRepository.save(newClaim);

    log.debug("Created new claim for Partner ID {} and Lab ID {}", partnerId, labId);

    return true;
  }

  /**
   * Gets the amount of remaining LabId Claims a Partner can claim.
   *
   * @param partnerId the partner Id
   * @return the amount of remaining claims.
   */
  public int getRemainingClaims(String partnerId) {
    return config.getLabIdClaim().getClaimsPerPartner() - labIdClaimRepository.countByPartnerId(partnerId);
  }

  /**
   * Returns the LabIdClaim Entity.
   *
   * @param labId to search
   * @return The entity.
   */
  public Optional<LabIdClaim> getClaimEntity(String labId) {
    return labIdClaimRepository.findByLabId(labId);
  }
}
