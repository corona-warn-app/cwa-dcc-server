/*
 * Corona-Warn-App / cwa-dcc
 *
 * (C) 2020, T-Systems International GmbH
 *
 * Deutsche Telekom AG and all other contributors /
 * copyright owners license this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package app.coronawarn.dcc.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Required;

@Schema(
  description = "Request payload to upload a Public Key."
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UploadPublicKeyRequest {

  @Pattern(regexp = "^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12}$")
  @Schema(description = "registrationToken from Verification Server", required = true)
  private String registrationToken;

  @NotNull
  @Schema(description = "Base64 encoded public key in DER format to encrypt DCC payload components.", required = true)
  private String publicKey;

  @JsonInclude(JsonInclude.Include.NON_NULL)
  @Transient
  private String responsePadding;

}
