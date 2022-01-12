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

package app.coronawarn.dcc.utils;

import app.coronawarn.dcc.model.RegistrationToken;
import feign.Request;
import feign.RequestTemplate;
import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;
import org.springframework.security.crypto.codec.Hex;

public class TestValues {

  public static final String registrationTokenValue = UUID.randomUUID().toString();
  public static final byte[] encryptedDek = new byte[]{1, 2, 3, 4, 5};
  public static final byte[] encryptedDcc = new byte[]{6, 7, 8, 9, 10};
  public static final String encryptedDekBase64 = Base64.getEncoder().encodeToString(encryptedDek);
  public static final String encryptedDccBase64 = Base64.getEncoder().encodeToString(encryptedDcc);
  public static final byte[] partialDcc = TestUtils.generatePartialDcc();
  public static final String partialDccBase64 = Base64.getEncoder().encodeToString(partialDcc);
  public static final String dccHash = "b".repeat(64);
  public static final String dccHashBase64 = Base64.getEncoder().encodeToString(Hex.decode(dccHash));
  public static final String testId = "d".repeat(64);
  public static final String labId = "e".repeat(64);
  public static final String partnerId = "f".repeat(64);
  public static final RegistrationToken registrationToken = new RegistrationToken(registrationTokenValue);
  public static final Request dummyRequest =
    Request.create(Request.HttpMethod.GET, "url", new HashMap<>(), null, new RequestTemplate());

  public static final String X_CWA_PARTNER_ID = "X-CWA-PARTNER-ID";
  public static final String X_CWA_REMAINING_LAB_ID = "X-CWA-REMAINING-LAB-ID";

}
