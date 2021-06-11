package app.coronawarn.dcc.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.model.RegistrationToken;
import app.coronawarn.dcc.repository.DccRegistrationRepository;
import app.coronawarn.dcc.service.DccRegistrationService;
import app.coronawarn.dcc.utils.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.KeyPair;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class ExternalDccClaimControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  DccRegistrationRepository dccRegistrationRepository;

  @Autowired
  DccRegistrationService dccRegistrationService;

  private static final String registrationTokenValue = "a".repeat(64);
  private static final RegistrationToken registrationToken = new RegistrationToken(registrationTokenValue);

  @Test
  void testClaimDcc() throws Exception {

    KeyPair keyPair = TestUtils.generateKeyPair();
    dccRegistrationService.createDccRegistration(registrationTokenValue, keyPair.getPublic());
    DccRegistration dccRegistration = dccRegistrationRepository.findByRegistrationToken(registrationTokenValue).orElseThrow();

    mockMvc.perform(post("/version/v1/dcc")
    .content(new ObjectMapper().writeValueAsString(registrationToken))
    );
  }

}
