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

import app.coronawarn.dcc.client.VerificationServerClient;
import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.model.InternalTestResult;
import app.coronawarn.dcc.model.LabTestResult;
import app.coronawarn.dcc.model.RegistrationToken;
import app.coronawarn.dcc.repository.DccRegistrationRepository;
import feign.FeignException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DccRegistrationService {

  private final DccRegistrationRepository dccRegistrationRepository;

  private final VerificationServerClient verificationServerClient;

  private final DcciGeneratorService dcciGeneratorService;

  public void createDccRegistration(String registrationToken, PublicKey publicKey) throws DccRegistrationException {
    checkRegistrationTokenAlreadyExists(registrationToken);
    InternalTestResult testResult = checkRegistrationTokenIsValid(registrationToken);

    DccRegistration dccRegistration = DccRegistration.builder()
      .registrationToken(registrationToken)
      .publicKey(Base64.getEncoder().encodeToString(publicKey.getEncoded()))
      .hashedGuid(testResult.getTestId())
      .labId(testResult.getLabId())
      .dcci(dcciGeneratorService.newDcci())
      .build();

    dccRegistrationRepository.save(dccRegistration);

    log.info("Saved new DCC Registration for RegistrationToken {}", registrationToken);
  }

  public List<DccRegistration> findByLabId(String labId) {
    return dccRegistrationRepository.findByLabId(labId);
  }

  private void checkRegistrationTokenAlreadyExists(String registrationToken) throws DccRegistrationException {
    Optional<DccRegistration> registrationOptional =
      dccRegistrationRepository.findByRegistrationToken(registrationToken);

    if (registrationOptional.isPresent()) {
      log.error("A DCC Registration already exists for RegistrationToken {}", registrationToken);
      throw new DccRegistrationException(DccRegistrationException.Reason.REGISTRATION_TOKEN_ALREADY_EXISTS);
    }
  }

  private InternalTestResult checkRegistrationTokenIsValid(String registrationToken) throws DccRegistrationException {
    InternalTestResult testResult;

    try {
      testResult = verificationServerClient.result(new RegistrationToken(registrationToken));
    } catch (FeignException e) {
      log.info("Failed to validate registrationToken. Http Status from Verification Server: {}", e.status());

      if (e.status() == HttpStatus.FORBIDDEN.value()) {
        throw new DccRegistrationException(DccRegistrationException.Reason.INVALID_REGISTRATION_TOKEN_FORBIDDEN);
      } else if (e.status() == HttpStatus.NOT_FOUND.value()) {
        throw new DccRegistrationException(DccRegistrationException.Reason.INVALID_REGISTRATION_TOKEN_NOT_FOUND);
      } else {
        throw new DccRegistrationException(DccRegistrationException.Reason.VERIFICATION_SERVER_ERROR);
      }
    }

    if (testResult.getTestResult() == LabTestResult.PENDING.ordinal()
      || testResult.getTestResult() == LabTestResult.QUICK_PENDING.ordinal()) {
      throw new DccRegistrationException(DccRegistrationException.Reason.INVALID_REGISTRATION_TOKEN_FORBIDDEN);
    }

    return testResult;
  }

  /**
   * Parse base64 encoded public key.
   * Supported algorithm: RSA, EC
   *
   * @param publicKeyBase64 base64 encoded public key.
   * @return Java PublicKey object or null if parsing failed.
   */
  public PublicKey parsePublicKey(String publicKeyBase64) {
    byte[] decoded = Base64.getDecoder().decode(publicKeyBase64);

    // Parse PublicKey as RSA Key
    PublicKey publicKey = parsePublicKey(decoded, "rsa");

    // Parse PublicKey as EC Key
    publicKey = publicKey != null ? publicKey : parsePublicKey(decoded, "ec");

    if (publicKey == null) {
      log.info("Could not parse PublicKey with any algorithm");
      return null;
    } else {
      return publicKey;
    }
  }

  private PublicKey parsePublicKey(byte[] publicKeyBytes, String alg) {
    KeyFactory keyFactory;
    try {
      keyFactory = KeyFactory.getInstance(alg);
      return keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));

    } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
      log.debug("Could not parse PublicKey with algorithm {}", alg);
      return null;
    }
  }

  public static class DccRegistrationException extends Exception {

    @Getter
    Reason reason;

    DccRegistrationException(Reason reason) {
      super();
      this.reason = reason;
    }

    public enum Reason {
      REGISTRATION_TOKEN_ALREADY_EXISTS,
      INVALID_REGISTRATION_TOKEN_NOT_FOUND,
      INVALID_REGISTRATION_TOKEN_FORBIDDEN,
      VERIFICATION_SERVER_ERROR
    }
  }

}