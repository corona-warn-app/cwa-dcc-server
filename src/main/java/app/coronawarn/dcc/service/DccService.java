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

import app.coronawarn.dcc.client.SigningApiClient;
import app.coronawarn.dcc.domain.DccErrorReason;
import app.coronawarn.dcc.domain.DccRegistration;
import com.upokecenter.cbor.CBORObject;
import feign.FeignException;
import java.util.Base64;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DccService {

  private final DccRegistrationService dccRegistrationService;

  private final SigningApiClient signingApiClient;

  /**
   * Creates signed data for a DCCRegistration.
   * This Endpoints queries the SigningAPI and signs the hash assigned to this Registration.
   * The resulting partialDCC will bestored in DCCRegistration.
   *
   * @param registration the DccRegistration to sign the DCC for.
   * @return DccRegistration
   * @throws DccGenerateException if signing went wrong.
   */
  public DccRegistration sign(DccRegistration registration) throws DccGenerateException {

    byte[] coseBytes;
    try {
      byte[] hashBytes = Hex.decode(registration.getDccHash());
      String hashBase64 = Base64.getEncoder().encodeToString(hashBytes);

      coseBytes = callSigningApiWithRetry(hashBase64);
    } catch (FeignException e) {
      log.error("Failed to sign DCC. Http Status Code: {}, Message: {}", e.status(), e.getMessage());

      if (e.status() > 0 && HttpStatus.valueOf(e.status()).is4xxClientError()) {
        dccRegistrationService.setError(registration, DccErrorReason.SIGNING_CLIENT_ERROR);
        throw new DccGenerateException(DccErrorReason.SIGNING_CLIENT_ERROR);
      } else {
        dccRegistrationService.setError(registration, DccErrorReason.SIGNING_SERVER_ERROR);
        throw new DccGenerateException(DccErrorReason.SIGNING_SERVER_ERROR);
      }
    }

    dccRegistrationService.setDcc(registration, Base64.getEncoder().encodeToString(coseBytes));

    // Reset Error if everything is ok and it previously exists
    if (registration.getError() != null) {
      dccRegistrationService.setError(registration, null);
    }

    return registration;
  }

  public byte[] callSigningApiWithRetry(String hashBase64) {
    try {
      return signingApiClient.sign(hashBase64);
    } catch (FeignException e) {
      log.info("First try of calling Signing API failed. Status Code: {}, Message: {}", e.status(), e.getMessage());
      return signingApiClient.sign(hashBase64);
    }
  }


  /**
   * Parses a COSE SIGN1_MESSAGE and replaces the payload Binary-String with a new Payload.
   *
   * @param dcc        the base64 encoded COSE SIGN1_MESSAGE
   * @param newPayload the base64 encoded new payload
   * @return base64 encoded COSE SIGN1_MESSAGE
   */
  public String replaceDccPayload(String dcc, String newPayload) {
    byte[] newPayloadBytes = Base64.getDecoder().decode(newPayload);

    CBORObject cbor = CBORObject.DecodeFromBytes(Base64.getDecoder().decode(dcc));
    cbor.set(2, CBORObject.FromObject(newPayloadBytes));

    return Base64.getEncoder().encodeToString(cbor.EncodeToBytes());

  }

  public static class DccGenerateException extends Exception {

    @Getter
    DccErrorReason reason;

    DccGenerateException(DccErrorReason reason) {
      super();
      this.reason = reason;
    }
  }

}
