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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import app.coronawarn.dcc.client.VerificationServerClient;
import app.coronawarn.dcc.domain.DccErrorReason;
import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.model.InternalTestResult;
import app.coronawarn.dcc.model.RegistrationToken;
import app.coronawarn.dcc.repository.DccRegistrationRepository;
import app.coronawarn.dcc.utils.TestUtils;
import feign.FeignException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class DccRegistrationServiceTest {

  @Autowired
  DccRegistrationService dccRegistrationService;

  @Autowired
  DccRegistrationRepository dccRegistrationRepository;

  @MockBean
  VerificationServerClient verificationServerClientMock;

  @BeforeEach
  void setup() {
    dccRegistrationRepository.deleteAll();
  }

  @Test
  void testCreateRegistration() throws NoSuchAlgorithmException {
    when(verificationServerClientMock.result(eq(registrationToken)))
      .thenReturn(new InternalTestResult(6, labId, testId, 0));

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();
    String publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());

    Assertions.assertDoesNotThrow(
      () -> dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey));

    Optional<DccRegistration> optional = dccRegistrationRepository.findByRegistrationToken(registrationTokenValue);

    Assertions.assertTrue(optional.isPresent());

    DccRegistration dccRegistrationInDb = optional.get();

    Assertions.assertEquals(labId, dccRegistrationInDb.getLabId());
    Assertions.assertEquals(testId, dccRegistrationInDb.getHashedGuid());
    Assertions.assertNull(dccRegistrationInDb.getPartnerId());
    Assertions.assertEquals(registrationTokenValue, dccRegistrationInDb.getRegistrationToken());
    Assertions.assertNotNull(dccRegistrationInDb.getDcci());
    Assertions.assertEquals(publicKeyBase64, dccRegistrationInDb.getPublicKey());
    Assertions.assertNull(dccRegistrationInDb.getEncryptedDataEncryptionKey());
    Assertions.assertNull(dccRegistrationInDb.getDccHash());
    Assertions.assertNull(dccRegistrationInDb.getDccEncryptedPayload());
    Assertions.assertNull(dccRegistrationInDb.getDcc());
    Assertions.assertNull(dccRegistrationInDb.getError());
    Assertions.assertEquals(1, dccRegistrationRepository.count());
  }

  @Test
  void testCreateRegistrationAlreadyExists() throws NoSuchAlgorithmException {
    when(verificationServerClientMock.result(eq(registrationToken)))
      .thenReturn(new InternalTestResult(6, labId, testId, 0));

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();

    Assertions.assertDoesNotThrow(
      () -> dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey));

    DccRegistrationService.DccRegistrationException e = Assertions.assertThrows(DccRegistrationService.DccRegistrationException.class,
      () -> dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey));

    Assertions.assertEquals(DccRegistrationService.DccRegistrationException.Reason.REGISTRATION_TOKEN_ALREADY_EXISTS, e.getReason());
    Assertions.assertEquals(1, dccRegistrationRepository.count());
  }

  @Test
  void testCreateRegistrationRegTokenNotFound() throws NoSuchAlgorithmException {
    doThrow(new FeignException.NotFound("", dummyRequest, null, null))
      .when(verificationServerClientMock).result(eq(registrationToken));

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();

    DccRegistrationService.DccRegistrationException e = Assertions.assertThrows(DccRegistrationService.DccRegistrationException.class,
      () -> dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey));

    Assertions.assertEquals(DccRegistrationService.DccRegistrationException.Reason.INVALID_REGISTRATION_TOKEN_NOT_FOUND, e.getReason());
    Assertions.assertEquals(0, dccRegistrationRepository.count());
  }

  @Test
  void testCreateRegistrationRegTokenForbidden() throws NoSuchAlgorithmException {
    doThrow(new FeignException.Forbidden("", dummyRequest, null, null))
      .when(verificationServerClientMock).result(eq(registrationToken));

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();

    DccRegistrationService.DccRegistrationException e = Assertions.assertThrows(DccRegistrationService.DccRegistrationException.class,
      () -> dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey));

    Assertions.assertEquals(DccRegistrationService.DccRegistrationException.Reason.INVALID_REGISTRATION_TOKEN_FORBIDDEN, e.getReason());
    Assertions.assertEquals(0, dccRegistrationRepository.count());
  }

  @Test
  void testCreateRegistrationVerificationServerError() throws NoSuchAlgorithmException {
    doThrow(new FeignException.InternalServerError("", dummyRequest, null, null))
      .when(verificationServerClientMock).result(eq(registrationToken));

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();

    DccRegistrationService.DccRegistrationException e = Assertions.assertThrows(DccRegistrationService.DccRegistrationException.class,
      () -> dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey));

    Assertions.assertEquals(DccRegistrationService.DccRegistrationException.Reason.VERIFICATION_SERVER_ERROR, e.getReason());
    Assertions.assertEquals(0, dccRegistrationRepository.count());
  }

  @Test
  void testCreateRegistrationPending() throws NoSuchAlgorithmException {
    when(verificationServerClientMock.result(eq(registrationToken)))
      .thenReturn(new InternalTestResult(0, labId, testId, 0));

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();

    DccRegistrationService.DccRegistrationException e = Assertions.assertThrows(DccRegistrationService.DccRegistrationException.class,
      () -> dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey));

    Assertions.assertEquals(DccRegistrationService.DccRegistrationException.Reason.INVALID_REGISTRATION_TOKEN_FORBIDDEN, e.getReason());
    Assertions.assertEquals(0, dccRegistrationRepository.count());
  }


  @Test
  void testCreateRegistrationQuicktestPending() throws NoSuchAlgorithmException {
    when(verificationServerClientMock.result(eq(registrationToken)))
      .thenReturn(new InternalTestResult(5, labId, testId, 0));

    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();

    DccRegistrationService.DccRegistrationException e = Assertions.assertThrows(DccRegistrationService.DccRegistrationException.class,
      () -> dccRegistrationService.createDccRegistration(registrationTokenValue, publicKey));

    Assertions.assertEquals(DccRegistrationService.DccRegistrationException.Reason.INVALID_REGISTRATION_TOKEN_FORBIDDEN, e.getReason());
    Assertions.assertEquals(0, dccRegistrationRepository.count());
  }

  @Test
  void testFindPendingDcc() throws NoSuchAlgorithmException, DccRegistrationService.DccRegistrationException {

    String registrationToken2 = "0".repeat(36);
    String registrationToken3 = "1".repeat(36);
    String registrationToken4 = "2".repeat(36);

    String testId2 = "0".repeat(64);
    String testId3 = "1".repeat(64);
    String testId4 = "3".repeat(64);

    when(verificationServerClientMock.result(eq(registrationToken))).thenReturn(new InternalTestResult(6, labId, testId, 0));
    when(verificationServerClientMock.result(eq(new RegistrationToken(registrationToken2)))).thenReturn(new InternalTestResult(6, labId, testId2, 0));
    when(verificationServerClientMock.result(eq(new RegistrationToken(registrationToken3)))).thenReturn(new InternalTestResult(6, labId, testId3, 0));
    when(verificationServerClientMock.result(eq(new RegistrationToken(registrationToken4)))).thenReturn(new InternalTestResult(6, labId, testId4, 0));

    dccRegistrationService.createDccRegistration(registrationTokenValue, TestUtils.generateKeyPair().getPublic());
    dccRegistrationService.createDccRegistration(registrationToken2, TestUtils.generateKeyPair().getPublic());
    dccRegistrationService.createDccRegistration(registrationToken3, TestUtils.generateKeyPair().getPublic());
    dccRegistrationService.createDccRegistration(registrationToken4, TestUtils.generateKeyPair().getPublic());

    List<DccRegistration> registrations = dccRegistrationService.findPendingDccByLabId(labId);

    Assertions.assertEquals(4, registrations.size());
    Assertions.assertEquals(testId, registrations.get(0).getHashedGuid());
    Assertions.assertEquals(testId2, registrations.get(1).getHashedGuid());
    Assertions.assertEquals(testId3, registrations.get(2).getHashedGuid());
    Assertions.assertEquals(testId4, registrations.get(3).getHashedGuid());

    DccRegistration registration2 = dccRegistrationRepository.findByRegistrationToken(registrationToken2).orElseThrow();
    registration2.setDccHash("h");
    dccRegistrationRepository.save(registration2);

    DccRegistration registration4 = dccRegistrationRepository.findByRegistrationToken(registrationToken4).orElseThrow();
    registration4.setDccHash("h");
    dccRegistrationRepository.save(registration4);

    registrations = dccRegistrationService.findPendingDccByLabId(labId);

    Assertions.assertEquals(2, registrations.size());
    Assertions.assertEquals(testId, registrations.get(0).getHashedGuid());
    Assertions.assertEquals(testId3, registrations.get(1).getHashedGuid());
  }

  @Test
  void testFindByHashedGuid() throws NoSuchAlgorithmException, DccRegistrationService.DccRegistrationException {

    String registrationToken2 = "0".repeat(36);
    String testId2 = "0".repeat(64);

    when(verificationServerClientMock.result(eq(registrationToken))).thenReturn(new InternalTestResult(6, labId, testId, 0));
    when(verificationServerClientMock.result(eq(new RegistrationToken(registrationToken2)))).thenReturn(new InternalTestResult(6, labId, testId2, 0));

    dccRegistrationService.createDccRegistration(registrationTokenValue, TestUtils.generateKeyPair().getPublic());
    dccRegistrationService.createDccRegistration(registrationToken2, TestUtils.generateKeyPair().getPublic());

    Assertions.assertEquals(testId, dccRegistrationService.findByHashedGuid(testId).orElseThrow().getHashedGuid());
  }

  @Test
  void testFindByRegistrationToken() throws NoSuchAlgorithmException, DccRegistrationService.DccRegistrationException {

    String registrationToken2 = "0".repeat(36);
    String testId2 = "0".repeat(64);

    when(verificationServerClientMock.result(eq(registrationToken))).thenReturn(new InternalTestResult(6, labId, testId, 0));
    when(verificationServerClientMock.result(eq(new RegistrationToken(registrationToken2)))).thenReturn(new InternalTestResult(6, labId, testId2, 0));

    dccRegistrationService.createDccRegistration(registrationTokenValue, TestUtils.generateKeyPair().getPublic());
    dccRegistrationService.createDccRegistration(registrationToken2, TestUtils.generateKeyPair().getPublic());

    Assertions.assertEquals(registrationTokenValue, dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow().getRegistrationToken());
  }

  @Test
  void testUpdateDccRegistration() throws NoSuchAlgorithmException, DccRegistrationService.DccRegistrationException {
    when(verificationServerClientMock.result(eq(registrationToken))).thenReturn(new InternalTestResult(6, labId, testId, 0));
    dccRegistrationService.createDccRegistration(registrationTokenValue, TestUtils.generateKeyPair().getPublic());

    DccRegistration registration = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();

    dccRegistrationService.updateDccRegistration(
      registration,
      dccHash,
      Base64.getEncoder().encodeToString(encryptedDcc),
      Base64.getEncoder().encodeToString(encryptedDek),
      partnerId);

    registration = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();

    Assertions.assertEquals(dccHash, registration.getDccHash());
    Assertions.assertEquals(Base64.getEncoder().encodeToString(encryptedDcc), registration.getDccEncryptedPayload());
    Assertions.assertEquals(Base64.getEncoder().encodeToString(encryptedDek), registration.getEncryptedDataEncryptionKey());
    Assertions.assertEquals(partnerId, registration.getPartnerId());
  }

  @Test
  void testDccRegistrationSetError() throws NoSuchAlgorithmException, DccRegistrationService.DccRegistrationException {
    when(verificationServerClientMock.result(eq(registrationToken))).thenReturn(new InternalTestResult(6, labId, testId, 0));
    dccRegistrationService.createDccRegistration(registrationTokenValue, TestUtils.generateKeyPair().getPublic());

    DccRegistration registration = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();
    Assertions.assertNull(registration.getError());

    dccRegistrationService.setError(
      registration,
      DccErrorReason.SIGNING_SERVER_ERROR);

    registration = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();

    Assertions.assertEquals(DccErrorReason.SIGNING_SERVER_ERROR, registration.getError());
  }

  @Test
  void testDccRegistrationSetDcc() throws NoSuchAlgorithmException, DccRegistrationService.DccRegistrationException {
    when(verificationServerClientMock.result(eq(registrationToken))).thenReturn(new InternalTestResult(6, labId, testId, 0));
    dccRegistrationService.createDccRegistration(registrationTokenValue, TestUtils.generateKeyPair().getPublic());

    DccRegistration registration = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();
    Assertions.assertNull(registration.getDcc());

    dccRegistrationService.setDcc(
      registration,
      Base64.getEncoder().encodeToString(partialDcc));

    registration = dccRegistrationService.findByRegistrationToken(registrationTokenValue).orElseThrow();

    Assertions.assertEquals(Base64.getEncoder().encodeToString(partialDcc), registration.getDcc());
  }

  @Test
  void testParsePublicKey() throws NoSuchAlgorithmException {
    PublicKey publicKey = TestUtils.generateKeyPair().getPublic();

    PublicKey parsedPublicKey = dccRegistrationService.parsePublicKey(Base64.getEncoder().encodeToString(publicKey.getEncoded()));

    Assertions.assertEquals(publicKey, parsedPublicKey);
  }

  @Test
  void testParsePublicKeyFail() {
    PublicKey parsedPublicKey = dccRegistrationService.parsePublicKey(Base64.getEncoder().encodeToString(encryptedDek));
    Assertions.assertNull(parsedPublicKey);
  }
}
