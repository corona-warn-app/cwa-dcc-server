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

package app.coronawarn.dcc.controller;

import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.exception.DccServerException;
import app.coronawarn.dcc.model.LabPublicKeyInfo;
import app.coronawarn.dcc.service.DccRegistrationService;
import app.coronawarn.dcc.service.LabIdClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("internal")
@RestController
@RequestMapping("/version/v1/publicKey")
@RequiredArgsConstructor
public class InternalPublicKeyController {

  private final DccRegistrationService dccRegistrationService;

  private final LabIdClaimService labIdClaimService;

  /**
   * Endpoint for inserting new DCC Registrations.
   */
  @Operation(
    summary = "Search Public Keys and Test Ids for given LabId",
    description = "Endpoint to search and download all PublicKeys which are assigned to a testId and the given lab Id.",
    tags = {"internal"},
    parameters = {
      @Parameter(name = "X-CWA-PARTNER-ID", in = ParameterIn.HEADER, description = "PartnerID. This needs only to be"
        + " set if DCC-Server is contacted without DCC-Proxy in between."),
      @Parameter(name = "labId", in = ParameterIn.PATH, description = "ID of the laboratory to search for.")
    },
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "Public Key list returned.",
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          array = @ArraySchema(schema = @Schema(implementation = LabPublicKeyInfo.class)))),
      @ApiResponse(responseCode = "500", description = "Internal Server Error"),
      @ApiResponse(responseCode = "403", description = "LabId is not or cannot be assigned to Partner.")
    })
  @GetMapping("/search/{labId}")
  public ResponseEntity<List<LabPublicKeyInfo>> searchPublicKeys(
    @Valid @Pattern(regexp = "^[A-Za-z0-9]{1,64}$") @RequestHeader("X-CWA-PARTNER-ID") String partnerId,
    @PathVariable("labId") String labId) {

    if (!labIdClaimService.getClaim(partnerId, labId)) {
      throw new DccServerException(HttpStatus.FORBIDDEN, "Failed to claim LabId");
    }

    List<LabPublicKeyInfo> resultList = dccRegistrationService.findPendingDccByLabId(labId).stream()
      .map(this::convert)
      .collect(Collectors.toList());

    return ResponseEntity.ok(resultList);
  }

  private LabPublicKeyInfo convert(DccRegistration dccRegistration) {
    LabPublicKeyInfo labPublicKeyInfo = new LabPublicKeyInfo();
    labPublicKeyInfo.setPublicKey(dccRegistration.getPublicKey());
    labPublicKeyInfo.setTestId(dccRegistration.getHashedGuid());
    labPublicKeyInfo.setDcci(dccRegistration.getDcci());

    return labPublicKeyInfo;
  }
}
