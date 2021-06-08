package app.coronawarn.dcc.service;

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

  /**
   * Checks if Claim already exists. If not creates a new one if quota is not already exceeded.
   *
   * @param partnerId the partner ID
   * @param labId     the lab ID
   * @return true if claiming was successful, false if not.
   */
  public boolean getClaim(String partnerId, String labId) {
    log.debug("Trying to get claim for partnerId {} and labId {}", partnerId, labId);

    Optional<LabIdClaim> claim = labIdClaimRepository.findByPartnerIdAndLabId(partnerId, labId);

    if (claim.isPresent()) {
      log.debug("Found Claim for partnerId {} and labId {} with ID {}", partnerId, labId, claim.get().getId());
      labIdClaimRepository.updateLastUsed(claim.get());
      return true;
    }

    log.debug("Could not find existing claim for partnerId {} and labID {}", partnerId, labId);

    int claimsOfPartnerID = labIdClaimRepository.countByPartnerId(partnerId);

    return true;
  }
}
