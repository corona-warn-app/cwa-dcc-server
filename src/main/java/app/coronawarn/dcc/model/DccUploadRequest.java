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
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(
  description = "Request payload to upload a DCC from laboratory."
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DccUploadRequest {

  @Schema(description = "SHA256 Hash of plain DCC payload.")
  @Pattern(regexp = "^[A-Fa-f0-9]{64}$")
  private String dccHash;

  @Schema(description = "Base64 encoded encrypted DCC payload.")
  @Pattern(regexp = "^[A-Za-z0-9+/=]*$")
  @Size(max = 1000)
  @NotNull
  @NotEmpty
  private String encryptedDcc;

  @Schema(description = "Base64 encoded with PublicKey encrypted Data Encryption Key for encrypted DCC.")
  @Pattern(regexp = "^[A-Za-z0-9+/=]*$")
  @Size(max = 255)
  @NotNull
  @NotEmpty
  private String dataEncryptionKey;

}
