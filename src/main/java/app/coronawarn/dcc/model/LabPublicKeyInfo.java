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

package app.coronawarn.dcc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(
  description = "Information a lab receives when searching for PublicKeys"
)
@Data
public class LabPublicKeyInfo {

  @Schema(description = "Hashed GUID of the test.")
  private String testId;

  @Schema(description = "The DCCI of the to be created DCC.")
  private String dcci;

  @Schema(
    description = "The PublicKey to encrypt the Data Encryption Key with.",
    format = "Base64 encoded X509 SubjectPublicKeyInformation Object (RSA or EC Key)")
  private String publicKey;

}
