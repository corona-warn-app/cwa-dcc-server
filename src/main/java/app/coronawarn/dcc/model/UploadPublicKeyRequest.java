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

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(
  description = "Request payload to upload a Public Key."
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadPublicKeyRequest {

  @Pattern(regexp = "^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}$")
  @Schema(description = "registrationToken from Verification Server", requiredMode = Schema.RequiredMode.REQUIRED)
  private String registrationToken;

  @NotNull
  @Schema(description = "Base64 encoded public key in DER format to encrypt DCC payload components.",
    requiredMode = Schema.RequiredMode.REQUIRED)
  private String publicKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Transient
  private String responsePadding;

}
