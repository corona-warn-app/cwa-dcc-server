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

import static app.coronawarn.dcc.utils.TestValues.dummyRequest;
import static app.coronawarn.dcc.utils.TestValues.labId;
import static app.coronawarn.dcc.utils.TestValues.partialDcc;
import static app.coronawarn.dcc.utils.TestValues.registrationToken;
import static app.coronawarn.dcc.utils.TestValues.registrationTokenValue;
import static app.coronawarn.dcc.utils.TestValues.testId;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.coronawarn.dcc.client.SigningApiClient;
import app.coronawarn.dcc.client.VerificationServerClient;
import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.model.InternalTestResult;
import app.coronawarn.dcc.model.UploadPublicKeyRequest;
import app.coronawarn.dcc.repository.DccRegistrationRepository;
import app.coronawarn.dcc.repository.LabIdClaimRepository;
import app.coronawarn.dcc.service.DccRegistrationService;
import app.coronawarn.dcc.utils.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.security.KeyPair;
import java.util.Base64;
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
@ActiveProfiles("external")
public class ExternalPublicKeyControllerTest {

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

  @BeforeEach
  void setup() {
    dccRegistrationRepository.deleteAll();
    labIdClaimRepository.deleteAll();

    when(verificationServerClientMock.result(eq(registrationToken)))
      .thenReturn(new InternalTestResult(6, labId, testId, 0));
  }

  @Test
  void testUploadPublicKey() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

    UploadPublicKeyRequest uploadPublicKeyRequest = new UploadPublicKeyRequest(
      registrationTokenValue, publicKeyBase64, "");

    mockMvc.perform(post("/version/v1/publicKey")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(uploadPublicKeyRequest))
    )
      .andExpect(status().isCreated());


    Optional<DccRegistration> dccRegistration = dccRegistrationRepository.findByRegistrationToken(registrationTokenValue);

    Assertions.assertTrue(dccRegistration.isPresent());
    Assertions.assertEquals(publicKeyBase64, dccRegistration.get().getPublicKey());
  }

  @Test
  void testUploadPublicKeyFailedInvalidPublicKey() throws Exception {
    String publicKeyBase64 = Base64.getEncoder().encodeToString(partialDcc);

    UploadPublicKeyRequest uploadPublicKeyRequest = new UploadPublicKeyRequest(
      registrationTokenValue, publicKeyBase64, "");

    mockMvc.perform(post("/version/v1/publicKey")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(uploadPublicKeyRequest))
    )
      .andExpect(status().isBadRequest());
  }

  @Test
  void testUploadPublicKeyInvalidRegistrationTokenForbidden() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

    UploadPublicKeyRequest uploadPublicKeyRequest = new UploadPublicKeyRequest(
      registrationTokenValue, publicKeyBase64, "");

    reset(verificationServerClientMock);

    doThrow(new FeignException.Forbidden("", dummyRequest, null, null))
      .when(verificationServerClientMock).result(eq(registrationToken));

    mockMvc.perform(post("/version/v1/publicKey")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(uploadPublicKeyRequest))
    )
      .andExpect(status().isForbidden());
  }


  @Test
  void testUploadPublicKeyInvalidRegistrationTokenNotFound() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

    UploadPublicKeyRequest uploadPublicKeyRequest = new UploadPublicKeyRequest(
      registrationTokenValue, publicKeyBase64, "");

    reset(verificationServerClientMock);

    doThrow(new FeignException.NotFound("", dummyRequest, null, null))
      .when(verificationServerClientMock).result(eq(registrationToken));

    mockMvc.perform(post("/version/v1/publicKey")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(uploadPublicKeyRequest))
    )
      .andExpect(status().isNotFound());
  }


  @Test
  void testUploadPublicKeyInvalidRegistrationTokenInternalServerError() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

    UploadPublicKeyRequest uploadPublicKeyRequest = new UploadPublicKeyRequest(
      registrationTokenValue, publicKeyBase64, "");

    reset(verificationServerClientMock);

    doThrow(new FeignException.InternalServerError("", dummyRequest, null, null))
      .when(verificationServerClientMock).result(eq(registrationToken));

    mockMvc.perform(post("/version/v1/publicKey")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(uploadPublicKeyRequest))
    )
      .andExpect(status().isInternalServerError());
  }

  @Test
  void testUploadPublicKeyFailedAlreadyExists() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());

    UploadPublicKeyRequest uploadPublicKeyRequest = new UploadPublicKeyRequest(
      registrationTokenValue, publicKeyBase64, "");

    mockMvc.perform(post("/version/v1/publicKey")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(uploadPublicKeyRequest))
    )
      .andExpect(status().isConflict());
  }

  @Test
  void testUploadPublicKeyFailedTestResultPending() throws Exception {
    KeyPair keyPair = TestUtils.generateKeyPair();
    String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());

    UploadPublicKeyRequest uploadPublicKeyRequest = new UploadPublicKeyRequest(
      registrationTokenValue, publicKeyBase64, "");

    reset(verificationServerClientMock);

    when(verificationServerClientMock.result(eq(registrationToken)))
      .thenReturn(new InternalTestResult(0, labId, testId, 0));

    mockMvc.perform(post("/version/v1/publicKey")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(uploadPublicKeyRequest))
    )
      .andExpect(status().isForbidden());
  }

}
