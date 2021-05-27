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

package app.coronawarn.dcc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(
  description = "Response with DCC data."
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DccDownloadResponse {

  @Schema(description = "Base64 encoded Data Encryption Key "
    + "(Encrypted AES-256 Key, encrypted with uploaders public key)")
  private String dek;

  @Schema(description = "Base64 encoded DCC COSE_SIGN1 Object. Payload is encrypted with data encryption key.")
  private String dcc;

}
