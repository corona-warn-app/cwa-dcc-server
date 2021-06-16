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
import static app.coronawarn.dcc.utils.TestValues.dccHash;
import static app.coronawarn.dcc.utils.TestValues.dccHashBase64;
import static app.coronawarn.dcc.utils.TestValues.dummyRequest;
import static app.coronawarn.dcc.utils.TestValues.encryptedDccBase64;
import static app.coronawarn.dcc.utils.TestValues.encryptedDekBase64;
import static app.coronawarn.dcc.utils.TestValues.labId;
import static app.coronawarn.dcc.utils.TestValues.partialDcc;
import static app.coronawarn.dcc.utils.TestValues.partialDccBase64;
import static app.coronawarn.dcc.utils.TestValues.partnerId;
import static app.coronawarn.dcc.utils.TestValues.registrationToken;
import static app.coronawarn.dcc.utils.TestValues.registrationTokenValue;
import static app.coronawarn.dcc.utils.TestValues.testId;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.coronawarn.dcc.client.SigningApiClient;
import app.coronawarn.dcc.client.VerificationServerClient;
import app.coronawarn.dcc.domain.DccErrorReason;
import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.model.DccUploadRequest;
import app.coronawarn.dcc.model.InternalTestResult;
import app.coronawarn.dcc.repository.DccRegistrationRepository;
import app.coronawarn.dcc.repository.LabIdClaimRepository;
import app.coronawarn.dcc.service.DccRegistrationService;
import app.coronawarn.dcc.service.LabIdClaimService;
import app.coronawarn.dcc.utils.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.security.KeyPair;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
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
public class InternalDccControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  DccRegistrationRepository dccRegistrationRepository;

  @Autowired
  LabIdClaimRepository labIdClaimRepository;

  @Autowired
  LabIdClaimService labIdClaimService;

  @Autowired
  DccRegistrationService dccRegistrationService;

  @MockBean
  VerificationServerClient verificationServerClientMock;

  @MockBean
  SigningApiClient signingApiClientMock;

  @BeforeEach
  void setup() {
    dccRegistrationRepository.deleteAll();
    labIdClaimRepository.deleteAll();

    when(verificationServerClientMock.result(eq(registrationToken)))
      .thenReturn(new InternalTestResult(6, labId, testId, 0));

    when(signingApiClientMock.sign(eq(dccHashBase64), eq(labId)))
      .thenReturn(partialDcc);
  }

  @Test
  void testUploadDcc() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();

    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());

    DccUploadRequest dccUploadRequest = new DccUploadRequest(dccHash, encryptedDccBase64, encryptedDekBase64);

    mockMvc.perform(post("/version/v1/test/" + testId + "/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .header(X_CWA_PARTNER_ID, partnerId)
      .content(new ObjectMapper().writeValueAsString(dccUploadRequest))
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.partialDcc").value(equalTo(partialDccBase64)));

    verify(signingApiClientMock).sign(eq(dccHashBase64), eq(labId));

    Optional<DccRegistration> dccRegistration = dccRegistrationRepository.findByRegistrationToken(registrationTokenValue);

    Assertions.assertTrue(dccRegistration.isPresent());
    Assertions.assertEquals(encryptedDccBase64, dccRegistration.get().getDccEncryptedPayload());
    Assertions.assertEquals(encryptedDekBase64, dccRegistration.get().getEncryptedDataEncryptionKey());
    Assertions.assertEquals(dccHash, dccRegistration.get().getDccHash());
    Assertions.assertEquals(partialDccBase64, dccRegistration.get().getDcc());
    Assertions.assertEquals(partnerId, dccRegistration.get().getPartnerId());
    Assertions.assertEquals(labId, dccRegistration.get().getLabId());
  }

  @Test
  void testUploadDccFailedUnknownTestId() throws Exception {
    DccUploadRequest dccUploadRequest = new DccUploadRequest(dccHash, encryptedDccBase64, encryptedDekBase64);

    mockMvc.perform(post("/version/v1/test/" + testId + "/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .header(X_CWA_PARTNER_ID, partnerId)
      .content(new ObjectMapper().writeValueAsString(dccUploadRequest))
    )
      .andExpect(status().isNotFound());

    verify(signingApiClientMock, never()).sign(eq(dccHashBase64), eq(labId));
  }

  @Test
  void testUploadDccFailedLabIdNotAssignedToPartner() throws Exception {
    DccUploadRequest dccUploadRequest = new DccUploadRequest(dccHash, encryptedDccBase64, encryptedDekBase64);

    KeyPair keyPair = TestUtils.generateKeyPair();
    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());

    Assertions.assertTrue(labIdClaimService.getClaim("otherpartner", labId));

    mockMvc.perform(post("/version/v1/test/" + testId + "/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .header(X_CWA_PARTNER_ID, partnerId)
      .content(new ObjectMapper().writeValueAsString(dccUploadRequest))
    )
      .andExpect(status().isForbidden());

    verify(signingApiClientMock, never()).sign(eq(dccHashBase64), eq(labId));
  }

  @Test
  void testUploadDccFailedAlreadyExists() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();

    DccRegistration dccRegistration = dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());

    dccRegistrationService.updateDccRegistration(dccRegistration, dccHash, encryptedDccBase64, encryptedDekBase64, partnerId);

    DccUploadRequest dccUploadRequest = new DccUploadRequest(dccHash, encryptedDccBase64, encryptedDekBase64);

    mockMvc.perform(post("/version/v1/test/" + testId + "/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .header(X_CWA_PARTNER_ID, partnerId)
      .content(new ObjectMapper().writeValueAsString(dccUploadRequest))
    )
      .andExpect(status().isConflict());

    verify(signingApiClientMock, never()).sign(eq(dccHashBase64), eq(labId));
  }

  @Test
  void testUploadDccFailedInvalidDCC() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());
    DccUploadRequest dccUploadRequest = new DccUploadRequest(dccHash, "Bad_Base64_String", encryptedDekBase64);

    mockMvc.perform(post("/version/v1/test/" + testId + "/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .header(X_CWA_PARTNER_ID, partnerId)
      .content(new ObjectMapper().writeValueAsString(dccUploadRequest))
    )
      .andExpect(status().isBadRequest());

    verify(signingApiClientMock, never()).sign(eq(dccHashBase64), eq(labId));
  }

  @Test
  void testUploadDccFailedInvalidDek() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());
    DccUploadRequest dccUploadRequest = new DccUploadRequest(dccHash, encryptedDccBase64, "Bad_Base64_String");

    mockMvc.perform(post("/version/v1/test/" + testId + "/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .header(X_CWA_PARTNER_ID, partnerId)
      .content(new ObjectMapper().writeValueAsString(dccUploadRequest))
    )
      .andExpect(status().isBadRequest());

    verify(signingApiClientMock, never()).sign(eq(dccHashBase64), eq(labId));
  }

  @Test
  void testUploadDccFailedBadResponseFromSignignAPI4xx() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());
    DccUploadRequest dccUploadRequest = new DccUploadRequest(dccHash, encryptedDccBase64, encryptedDekBase64);

    doThrow(new FeignException.BadRequest("", dummyRequest, null))
      .when(signingApiClientMock).sign(eq(dccHashBase64), eq(labId));

    mockMvc.perform(post("/version/v1/test/" + testId + "/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .header(X_CWA_PARTNER_ID, partnerId)
      .content(new ObjectMapper().writeValueAsString(dccUploadRequest))
    )
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.reason").value(equalTo(DccErrorReason.SIGNING_CLIENT_ERROR.toString())));

    Optional<DccRegistration> dccRegistration = dccRegistrationRepository.findByRegistrationToken(registrationTokenValue);
    Assertions.assertTrue(dccRegistration.isPresent());
    Assertions.assertNull(dccRegistration.get().getDccEncryptedPayload());
    Assertions.assertNull(dccRegistration.get().getEncryptedDataEncryptionKey());
    Assertions.assertNull(dccRegistration.get().getDccHash());
    Assertions.assertNull(dccRegistration.get().getDcc());
    Assertions.assertEquals(DccErrorReason.SIGNING_CLIENT_ERROR, dccRegistration.get().getError());
  }

  @Test
  void testUploadDccFailedBadResponseFromSignignAPI5xx() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());
    DccUploadRequest dccUploadRequest = new DccUploadRequest(dccHash, encryptedDccBase64, encryptedDekBase64);

    doThrow(new FeignException.InternalServerError("", dummyRequest, null))
      .when(signingApiClientMock).sign(eq(dccHashBase64), eq(labId));

    mockMvc.perform(post("/version/v1/test/" + testId + "/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .header(X_CWA_PARTNER_ID, partnerId)
      .content(new ObjectMapper().writeValueAsString(dccUploadRequest))
    )
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.reason").value(equalTo(DccErrorReason.SIGNING_SERVER_ERROR.toString())));

    Optional<DccRegistration> dccRegistration = dccRegistrationRepository.findByRegistrationToken(registrationTokenValue);
    Assertions.assertTrue(dccRegistration.isPresent());
    Assertions.assertNull(dccRegistration.get().getDccEncryptedPayload());
    Assertions.assertNull(dccRegistration.get().getEncryptedDataEncryptionKey());
    Assertions.assertNull(dccRegistration.get().getDccHash());
    Assertions.assertNull(dccRegistration.get().getDcc());
    Assertions.assertEquals(DccErrorReason.SIGNING_SERVER_ERROR, dccRegistration.get().getError());
  }
}
