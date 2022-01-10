/*-
 * ---license-start
 * Corona-Warn-App / cwa-dcc
 * ---
 * Copyright (C) 2020 - 2022 T-Systems International GmbH and all other contributors
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ---license-end
 */

package app.coronawarn.dcc.service;

import app.coronawarn.dcc.config.DccApplicationConfig;
import app.coronawarn.dcc.repository.LabIdClaimRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabIdClaimCleanupService {

  private final LabIdClaimRepository labIdClaimRepository;

  private final DccApplicationConfig config;

  /**
   * Cleanup Job to remove LabId claims which are not used anymore by partners.
   */
  @Scheduled(fixedDelayString = "${cwa.dcc.cleanup.rate:1800000}")
  @SchedulerLock(name = "labidclaim_cleanup_job")
  public void cleanup() {
    log.info("Start LabId Claim Cleanup...");

    LocalDateTime threshold = LocalDateTime.now().minus(config.getLabIdClaim().getMaximumAge(), ChronoUnit.DAYS);
    int deleteCount = labIdClaimRepository.deleteClaimsOlderThan(threshold);
    log.info("Deleted {} Lab ID Claims", deleteCount);

    log.info("Finished LabId Claim Cleanup.");
  }

}
