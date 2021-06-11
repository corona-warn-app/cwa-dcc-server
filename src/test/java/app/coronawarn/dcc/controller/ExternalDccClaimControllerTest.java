package app.coronawarn.dcc.controller;

import static app.coronawarn.dcc.utils.TestValues.dccHash;
import static app.coronawarn.dcc.utils.TestValues.encryptedDcc;
import static app.coronawarn.dcc.utils.TestValues.encryptedDek;
import static app.coronawarn.dcc.utils.TestValues.labId;
import static app.coronawarn.dcc.utils.TestValues.partialDcc;
import static app.coronawarn.dcc.utils.TestValues.partnerId;
import static app.coronawarn.dcc.utils.TestValues.registrationToken;
import static app.coronawarn.dcc.utils.TestValues.registrationTokenValue;
import static app.coronawarn.dcc.utils.TestValues.testId;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.coronawarn.dcc.client.SigningApiClient;
import app.coronawarn.dcc.client.VerificationServerClient;
import app.coronawarn.dcc.domain.DccErrorReason;
import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.model.InternalTestResult;
import app.coronawarn.dcc.repository.DccRegistrationRepository;
import app.coronawarn.dcc.repository.LabIdClaimRepository;
import app.coronawarn.dcc.service.DccRegistrationService;
import app.coronawarn.dcc.utils.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.upokecenter.cbor.CBORObject;
import java.security.KeyPair;
import java.util.Base64;
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
public class ExternalDccClaimControllerTest {

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
  void testClaimDcc() throws Exception {

    KeyPair keyPair = TestUtils.generateKeyPair();
    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());
    DccRegistration dccRegistration = dccRegistrationRepository.findByRegistrationToken(registrationTokenValue).orElseThrow();

    dccRegistration = dccRegistrationService.updateDccRegistration(
      dccRegistration,
      dccHash,
      Base64.getEncoder().encodeToString(encryptedDcc),
      Base64.getEncoder().encodeToString(encryptedDek),
      partnerId);

    dccRegistrationService.setDcc(dccRegistration, Base64.getEncoder().encodeToString(partialDcc));

    String expectedCbor = Base64.getEncoder().encodeToString(
      CBORObject.DecodeFromBytes(partialDcc)
        .Set(2, CBORObject.FromObject(encryptedDcc))
        .EncodeToBytes());

    mockMvc.perform(post("/version/v1/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(registrationToken))
    )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.dcc").value(equalTo(expectedCbor)))
      .andExpect(jsonPath("$.dek").value(equalTo(Base64.getEncoder().encodeToString(encryptedDek))));
  }

  @Test
  void testClaimDccFailedTokenNotFound() throws Exception {

    mockMvc.perform(post("/version/v1/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(registrationToken))
    )
      .andExpect(status().isNotFound());
  }

  @Test
  void testClaimDccFailedRegistrationHasErrorSet() throws Exception {

    KeyPair keyPair = TestUtils.generateKeyPair();
    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());
    DccRegistration dccRegistration = dccRegistrationRepository.findByRegistrationToken(registrationTokenValue).orElseThrow();

    dccRegistrationService.setError(dccRegistration, DccErrorReason.SIGNING_SERVER_ERROR);

    mockMvc.perform(post("/version/v1/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(registrationToken))
    )
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.reason").value(equalTo(DccErrorReason.SIGNING_SERVER_ERROR.toString())));
  }

  @Test
  void testClaimDccPending() throws Exception {

    KeyPair keyPair = TestUtils.generateKeyPair();
    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());

    mockMvc.perform(post("/version/v1/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(registrationToken))
    )
      .andExpect(status().isAccepted())
      .andExpect(content().bytes(new byte[0]));

  }

  @Test
  void testClaimDccGone() throws Exception {

    KeyPair keyPair = TestUtils.generateKeyPair();
    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());
    DccRegistration dccRegistration = dccRegistrationRepository.findByRegistrationToken(registrationTokenValue).orElseThrow();

    dccRegistrationService.updateDccRegistration(
      dccRegistration,
      dccHash,
      null,
      null,
      partnerId);

    mockMvc.perform(post("/version/v1/dcc")
      .contentType(MediaType.APPLICATION_JSON_VALUE)
      .content(new ObjectMapper().writeValueAsString(registrationToken))
    )
      .andExpect(status().isGone())
      .andExpect(content().bytes(new byte[0]));
  }

}
