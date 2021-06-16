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

package app.coronawarn.dcc.service;

import static app.coronawarn.dcc.utils.TestValues.dccHash;
import static app.coronawarn.dcc.utils.TestValues.dummyRequest;
import static app.coronawarn.dcc.utils.TestValues.encryptedDcc;
import static app.coronawarn.dcc.utils.TestValues.encryptedDek;
import static app.coronawarn.dcc.utils.TestValues.labId;
import static app.coronawarn.dcc.utils.TestValues.partialDcc;
import static app.coronawarn.dcc.utils.TestValues.partnerId;
import static app.coronawarn.dcc.utils.TestValues.registrationToken;
import static app.coronawarn.dcc.utils.TestValues.registrationTokenValue;
import static app.coronawarn.dcc.utils.TestValues.testId;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import app.coronawarn.dcc.client.SigningApiClient;
import app.coronawarn.dcc.client.VerificationServerClient;
import app.coronawarn.dcc.domain.DccErrorReason;
import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.model.InternalTestResult;
import app.coronawarn.dcc.repository.DccRegistrationRepository;
import app.coronawarn.dcc.utils.TestUtils;
import feign.FeignException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.codec.Hex;

@SpringBootTest
public class DccServiceTest {

  @Autowired
  DccRegistrationService dccRegistrationService;

  @Autowired
  DccService dccService;

  @Autowired
  DccRegistrationRepository dccRegistrationRepository;

  @Autowired
  HashingService hashingService;

  @MockBean
  VerificationServerClient verificationServerClientMock;

  @MockBean
  SigningApiClient signingApiClient;

  @BeforeEach
  void setup() {
    dccRegistrationRepository.deleteAll();
  }

  @Test
  void testSigning() throws NoSuchAlgorithmException, DccRegistrationService.DccRegistrationException {

    when(verificationServerClientMock.result(eq(registrationToken))).thenReturn(new InternalTestResult(6, labId, testId, 0));
    when(signingApiClient.sign(
      eq(Base64.getEncoder().encodeToString(Hex.decode(dccHash))),
      eq(hashingService.hash(labId)),
      anyString()
    )).thenReturn(partialDcc);

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();
    DccRegistration registration = dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey);
    DccRegistration updateDccRegistration = dccRegistrationService.updateDccRegistration(
      registration,
      dccHash,
      Base64.getEncoder().encodeToString(encryptedDcc),
      Base64.getEncoder().encodeToString(encryptedDek),
      partnerId);

    Assertions.assertDoesNotThrow(() -> dccService.sign(updateDccRegistration));

    registration = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();

    Assertions.assertNull(registration.getError());
    Assertions.assertArrayEquals(partialDcc, Base64.getDecoder().decode(registration.getDcc()));
  }

  @Test
  void testSigningFailedBySigningApi4xx() throws NoSuchAlgorithmException, DccRegistrationService.DccRegistrationException {

    when(verificationServerClientMock.result(eq(registrationToken))).thenReturn(new InternalTestResult(6, labId, testId, 0));

    doThrow(new FeignException.BadRequest("", dummyRequest, null))
      .when(signingApiClient).sign(
      eq(Base64.getEncoder().encodeToString(Hex.decode(dccHash))),
      eq(hashingService.hash(labId)),
      anyString());

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();
    DccRegistration registration = dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey);
    DccRegistration updateDccRegistration = dccRegistrationService.updateDccRegistration(
      registration,
      dccHash,
      Base64.getEncoder().encodeToString(encryptedDcc),
      Base64.getEncoder().encodeToString(encryptedDek),
      partnerId);

    DccService.DccGenerateException e =
      Assertions.assertThrows(DccService.DccGenerateException.class, () -> dccService.sign(updateDccRegistration));

    registration = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();
    Assertions.assertEquals(DccErrorReason.SIGNING_CLIENT_ERROR, e.getReason());
    Assertions.assertEquals(DccErrorReason.SIGNING_CLIENT_ERROR, registration.getError());
    Assertions.assertNull(registration.getDcc());
  }

  @Test
  void testSigningFailedBySigningApi5xx() throws NoSuchAlgorithmException, DccRegistrationService.DccRegistrationException {

    when(verificationServerClientMock.result(eq(registrationToken))).thenReturn(new InternalTestResult(6, labId, testId, 0));

    doThrow(new FeignException.InternalServerError("", dummyRequest, null))
      .when(signingApiClient).sign(
      eq(Base64.getEncoder().encodeToString(Hex.decode(dccHash))),
      eq(hashingService.hash((labId))),
      anyString());

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();
    DccRegistration registration = dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey);
    DccRegistration updateDccRegistration = dccRegistrationService.updateDccRegistration(
      registration,
      dccHash,
      Base64.getEncoder().encodeToString(encryptedDcc),
      Base64.getEncoder().encodeToString(encryptedDek),
      partnerId);

    DccService.DccGenerateException e =
      Assertions.assertThrows(DccService.DccGenerateException.class, () -> dccService.sign(updateDccRegistration));

    registration = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();
    Assertions.assertEquals(DccErrorReason.SIGNING_SERVER_ERROR, e.getReason());
    Assertions.assertEquals(DccErrorReason.SIGNING_SERVER_ERROR, registration.getError());
    Assertions.assertNull(registration.getDcc());
  }

  @Test
  void testSigningRetry() throws NoSuchAlgorithmException, DccRegistrationService.DccRegistrationException {

    when(verificationServerClientMock.result(eq(registrationToken))).thenReturn(new InternalTestResult(6, labId, testId, 0));

    doThrow(new FeignException.InternalServerError("", dummyRequest, null))
      .doReturn(partialDcc)
      .when(signingApiClient).sign(
      eq(Base64.getEncoder().encodeToString(Hex.decode(dccHash))),
      eq(hashingService.hash(labId)),
      anyString());

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();
    DccRegistration registration = dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey);
    DccRegistration updateDccRegistration = dccRegistrationService.updateDccRegistration(
      registration,
      dccHash,
      Base64.getEncoder().encodeToString(encryptedDcc),
      Base64.getEncoder().encodeToString(encryptedDek),
      partnerId);

    Assertions.assertDoesNotThrow(() -> dccService.sign(updateDccRegistration));

    registration = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();

    Assertions.assertNull(registration.getError());
    Assertions.assertArrayEquals(partialDcc, Base64.getDecoder().decode(registration.getDcc()));
  }

  @Test
  void testSigningFailedOnFirstRequestButSuccessOnSecond() throws NoSuchAlgorithmException, DccRegistrationService.DccRegistrationException {

    when(verificationServerClientMock.result(eq(registrationToken))).thenReturn(new InternalTestResult(6, labId, testId, 0));

    doThrow(new FeignException.InternalServerError("", dummyRequest, null))
      .doThrow(new FeignException.InternalServerError("", dummyRequest, null))
      .doReturn(partialDcc)
      .when(signingApiClient).sign(
      eq(Base64.getEncoder().encodeToString(Hex.decode(dccHash))),
      eq(hashingService.hash(labId)),
      anyString());

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();
    DccRegistration registration = dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey);
    DccRegistration updatedDccRegistration = dccRegistrationService.updateDccRegistration(
      registration,
      dccHash,
      Base64.getEncoder().encodeToString(encryptedDcc),
      Base64.getEncoder().encodeToString(encryptedDek),
      partnerId);

    DccService.DccGenerateException e =
      Assertions.assertThrows(DccService.DccGenerateException.class, () -> dccService.sign(updatedDccRegistration));

    DccRegistration registrationWithFailedSigning = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();
    Assertions.assertEquals(DccErrorReason.SIGNING_SERVER_ERROR, e.getReason());
    Assertions.assertEquals(DccErrorReason.SIGNING_SERVER_ERROR, registrationWithFailedSigning.getError());
    Assertions.assertNull(registrationWithFailedSigning.getDcc());

    registrationWithFailedSigning.setDccHash(dccHash);
    registrationWithFailedSigning.setDccEncryptedPayload(Base64.getEncoder().encodeToString(encryptedDcc));
    registrationWithFailedSigning.setEncryptedDataEncryptionKey(Base64.getEncoder().encodeToString(encryptedDek));
    registrationWithFailedSigning.setPartnerId(partnerId);

    Assertions.assertDoesNotThrow(() -> dccService.sign(registrationWithFailedSigning));

    registration = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();
    Assertions.assertNull(registration.getError());
    Assertions.assertArrayEquals(partialDcc, Base64.getDecoder().decode(registration.getDcc()));
  }

}
