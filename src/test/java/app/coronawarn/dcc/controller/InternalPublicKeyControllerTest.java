/*-
 * ---license-start
 * Corona-Warn-App / cwa-dcc
 * ---
 * Copyright (C) 2020 - 2021 T-Systems International GmbH and all other contributors
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
import static app.coronawarn.dcc.utils.TestValues.labId;
import static app.coronawarn.dcc.utils.TestValues.partnerId;
import static app.coronawarn.dcc.utils.TestValues.registrationToken;
import static app.coronawarn.dcc.utils.TestValues.registrationTokenValue;
import static app.coronawarn.dcc.utils.TestValues.testId;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.coronawarn.dcc.client.SigningApiClient;
import app.coronawarn.dcc.client.VerificationServerClient;
import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.model.InternalTestResult;
import app.coronawarn.dcc.model.RegistrationToken;
import app.coronawarn.dcc.repository.DccRegistrationRepository;
import app.coronawarn.dcc.repository.LabIdClaimRepository;
import app.coronawarn.dcc.service.DccRegistrationService;
import app.coronawarn.dcc.utils.TestUtils;
import java.security.KeyPair;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("internal")
public class InternalPublicKeyControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  DccRegistrationRepository dccRegistrationRepository;

  @Autowired
  LabIdClaimRepository labIdClaimRepository;

  @Autowired
  DccRegistrationService dccRegistrationService;

  @MockBean
  VerificationServerClient verificationServerClientMock;

  @MockBean
  SigningApiClient signingApiClientMock;

  private static final RegistrationToken registrationToken2 = new RegistrationToken(UUID.randomUUID().toString());
  private static final String testId2 = "12".repeat(32);
  private static final RegistrationToken registrationToken3 = new RegistrationToken(UUID.randomUUID().toString());
  private static final String testId3 = "13".repeat(32);
  private static final String otherLabId = "13".repeat(32);


  @BeforeEach
  void setup() {
    dccRegistrationRepository.deleteAll();
    labIdClaimRepository.deleteAll();

    when(verificationServerClientMock.result(eq(registrationToken)))
      .thenReturn(new InternalTestResult(6, labId, testId, 0));

    when(verificationServerClientMock.result(eq(registrationToken2)))
      .thenReturn(new InternalTestResult(6, otherLabId, testId2, 0));

    when(verificationServerClientMock.result(eq(registrationToken3)))
      .thenReturn(new InternalTestResult(6, labId, testId3, 0));
  }

  @Test
  void testDownloadPublicKeys() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    DccRegistration registration = dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());

    KeyPair keyPair2 = TestUtils.generateKeyPair();
    String publicKey2Base64 = Base64.getEncoder().encodeToString(keyPair2.getPublic().getEncoded());
    DccRegistration registration2 = dccRegistrationService.createDccRegistration(registrationToken2.getRegistrationToken(), keyPair2.getPublic());

    KeyPair keyPair3 = TestUtils.generateKeyPair();
    String publicKey3Base64 = Base64.getEncoder().encodeToString(keyPair3.getPublic().getEncoded());
    DccRegistration registration3 = dccRegistrationService.createDccRegistration(registrationToken3.getRegistrationToken(), keyPair3.getPublic());

    mockMvc.perform(get("/version/v1/publicKey/search/" + labId)
      .header(X_CWA_PARTNER_ID, partnerId)
      .contentType(MediaType.APPLICATION_JSON_VALUE)
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(equalTo(2)))
      .andExpect(jsonPath("$[0].testId").value(equalTo(testId)))
      .andExpect(jsonPath("$[0].dcci").value(equalTo(registration.getDcci())))
      .andExpect(jsonPath("$[0].publicKey").value(equalTo(publicKeyBase64)))
      .andExpect(jsonPath("$[1].testId").value(equalTo(testId3)))
      .andExpect(jsonPath("$[1].dcci").value(equalTo(registration3.getDcci())))
      .andExpect(jsonPath("$[1].publicKey").value(equalTo(publicKey3Base64)));

    mockMvc.perform(get("/version/v1/publicKey/search/" + otherLabId)
      .header(X_CWA_PARTNER_ID, partnerId)
      .contentType(MediaType.APPLICATION_JSON_VALUE)
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.length()").value(equalTo(1)))
      .andExpect(jsonPath("$[0].testId").value(equalTo(testId2)))
      .andExpect(jsonPath("$[0].dcci").value(equalTo(registration2.getDcci())))
      .andExpect(jsonPath("$[0].publicKey").value(equalTo(publicKey2Base64)));

    mockMvc.perform(get("/version/v1/publicKey/search/" + otherLabId)
      .header(X_CWA_PARTNER_ID, "otherPartnerId")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
    )
      .andExpect(status().isForbidden());
  }



}
