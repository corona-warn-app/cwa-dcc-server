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

package app.coronawarn.dcc.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * This class represents the Verification Server Feign client.
 */
@FeignClient(
  name = "signingApiClient",
  url = "${cwa.dcc.signing-api-server.base-url}",
  configuration = SigningApiClientConfig.class)
public interface SigningApiClient {

  /**
   * This method gets a COSE from UBirch API for a DCC.
   *
   * @param hash of unencrypted DCC.
   * @return Signed COSE from UBirch API
   */
  @PostMapping(value = "/api/certify/v2/issue/hash",
    consumes = MediaType.TEXT_PLAIN_VALUE,
    produces = MediaType.APPLICATION_CBOR_VALUE
  )
  byte[] sign(@RequestBody String hash);
}
