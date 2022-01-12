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

package app.coronawarn.dcc.controller;

import static app.coronawarn.dcc.utils.TestValues.X_CWA_PARTNER_ID;
import static app.coronawarn.dcc.utils.TestValues.X_CWA_REMAINING_LAB_ID;
import static app.coronawarn.dcc.utils.TestValues.labId;
import static app.coronawarn.dcc.utils.TestValues.partnerId;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.coronawarn.dcc.domain.LabIdClaim;
import app.coronawarn.dcc.model.LabIdClaimRequest;
import app.coronawarn.dcc.repository.LabIdClaimRepository;
import app.coronawarn.dcc.service.LabIdClaimService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "cwa.dcc.lab-id-claim.claims-per-partner=100")
@AutoConfigureMockMvc
@ActiveProfiles("internal")
public class InternalLabIdClaimControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  LabIdClaimRepository labIdClaimRepository;

  @Autowired
  LabIdClaimService labIdClaimService;

  private static final String labId2 = "12".repeat(32);
  private static final String partnerId2 = "22".repeat(32);

  @BeforeEach
  void setup() {
    labIdClaimRepository.deleteAll();
  }

  @Test
  void testClaimLabId() throws Exception {
    LabIdClaimRequest labIdClaimRequest = new LabIdClaimRequest(labId);

    mockMvc.perform(post("/version/v1/labId")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .header(X_CWA_PARTNER_ID, partnerId)
      .content(new ObjectMapper().writeValueAsString(labIdClaimRequest))
    )
      .andExpect(status().isNoContent())
      .andExpect(header().longValue(X_CWA_REMAINING_LAB_ID, 99));

    Optional<LabIdClaim> claim = labIdClaimRepository.findByLabId(labId);

    Assertions.assertTrue(claim.isPresent());
    Assertions.assertEquals(partnerId, claim.get().getPartnerId());
  }

  @Test
  void testClaimLabIdShouldNotWorkForAlreadyClaimedLabId() throws Exception {
    labIdClaimService.getClaim(partnerId, labId);

    LabIdClaimRequest labIdClaimRequest = new LabIdClaimRequest(labId);

    mockMvc.perform(post("/version/v1/labId")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .header(X_CWA_PARTNER_ID, partnerId2)
      .content(new ObjectMapper().writeValueAsString(labIdClaimRequest))
    )
      .andExpect(status().isConflict())
      .andExpect(header().doesNotExist(X_CWA_REMAINING_LAB_ID));

    Optional<LabIdClaim> claim = labIdClaimRepository.findByLabId(labId);

    Assertions.assertTrue(claim.isPresent());
    Assertions.assertEquals(partnerId, claim.get().getPartnerId());
  }

  @Test
  void testClaimLabIdShouldNotWorkWhenQuotaExceeded() throws Exception {

    for (int i = 0; i < 100; i++) {
      LabIdClaimRequest labIdClaimRequest = new LabIdClaimRequest(UUID.randomUUID().toString().replaceAll("-", ""));

      mockMvc.perform(post("/version/v1/labId")
        .contentType(MediaType.APPLICATION_JSON_VALUE)
        .header(X_CWA_PARTNER_ID, partnerId)
        .content(new ObjectMapper().writeValueAsString(labIdClaimRequest))
      )
        .andExpect(status().isNoContent())
        .andExpect(header().longValue(X_CWA_REMAINING_LAB_ID, 99 - i));
    }

    Assertions.assertEquals(100, labIdClaimRepository.count());

    LabIdClaimRequest labIdClaimRequest = new LabIdClaimRequest(labId);

    mockMvc.perform(post("/version/v1/labId")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .header(X_CWA_PARTNER_ID, partnerId)
      .content(new ObjectMapper().writeValueAsString(labIdClaimRequest))
    )
      .andExpect(status().isForbidden())
      .andExpect(header().doesNotExist(X_CWA_REMAINING_LAB_ID));

    Assertions.assertEquals(100, labIdClaimRepository.count());

  }


}
