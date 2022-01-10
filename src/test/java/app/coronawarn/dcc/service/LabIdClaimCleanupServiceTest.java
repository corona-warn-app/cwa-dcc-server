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

import static app.coronawarn.dcc.utils.TestValues.labId;
import static app.coronawarn.dcc.utils.TestValues.partnerId;

import app.coronawarn.dcc.domain.LabIdClaim;
import app.coronawarn.dcc.repository.LabIdClaimRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LabIdClaimCleanupServiceTest {

  @Autowired
  LabIdClaimRepository labIdClaimRepository;

  @Autowired
  LabIdClaimCleanupService labIdClaimCleanupService;

  @BeforeEach
  void setup() {
    labIdClaimRepository.deleteAll();
  }

  @Test
  void cleanupLabIdClaims() {
    LabIdClaim labIdClaim1 = new LabIdClaim(null, LocalDateTime.now(), LocalDateTime.now(), labId, partnerId);
    LabIdClaim labIdClaim2 = new LabIdClaim(null, LocalDateTime.now(), LocalDateTime.now(), "labId2", partnerId);
    LabIdClaim labIdClaim3 = new LabIdClaim(null, LocalDateTime.now(), LocalDateTime.now(), "labId3", partnerId);

    labIdClaim1 = labIdClaimRepository.save(labIdClaim1);
    labIdClaim2 = labIdClaimRepository.save(labIdClaim2);
    labIdClaim3 = labIdClaimRepository.save(labIdClaim3);

    labIdClaim1.setLastUsed(LocalDateTime.now().minus(31, ChronoUnit.DAYS));
    labIdClaim2.setLastUsed(LocalDateTime.now().minus(33, ChronoUnit.DAYS));
    labIdClaim3.setLastUsed(LocalDateTime.now().minus(29, ChronoUnit.DAYS));

    labIdClaimRepository.save(labIdClaim1);
    labIdClaimRepository.save(labIdClaim2);
    labIdClaimRepository.save(labIdClaim3);

    labIdClaimCleanupService.cleanup();

    Assertions.assertEquals(1, labIdClaimRepository.count());
    Assertions.assertEquals("labId3", labIdClaimRepository.findAll().get(0).getLabId());
  }

}
