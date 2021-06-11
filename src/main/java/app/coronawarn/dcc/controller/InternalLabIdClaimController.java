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

package app.coronawarn.dcc.controller;

import app.coronawarn.dcc.domain.LabIdClaim;
import app.coronawarn.dcc.model.LabIdClaimRequest;
import app.coronawarn.dcc.service.LabIdClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Optional;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("internal")
@RestController
@RequestMapping("/version/v1/labId")
@RequiredArgsConstructor
@Validated
public class InternalLabIdClaimController {

  private final LabIdClaimService labIdClaimService;

  private static final String REMAINING_LAB_ID_HEADER = "X-CWA-REMAINING-LAB-ID";

  /**
   * Endpoint for claiming a LabId.
   */
  @Operation(
    summary = "Claims a new LabId for partner.",
    description = "Checks if the given LabId is not in use and creates a claim to associate it with the partner.",
    tags = {"internal"},
    parameters = {
      @Parameter(name = "X-CWA-PARTNER-ID", in = ParameterIn.HEADER, description = "PartnerID. This needs only to be"
        + " set if DCC-Server is contacted without DCC-Proxy in between.")
    },
    requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = LabIdClaimRequest.class))),
    responses = {
      @ApiResponse(
        responseCode = "204",
        description = "LabId is claimed to you.",
        headers = @Header(
          name = REMAINING_LAB_ID_HEADER,
          description = "Amount of remaining LabIds which can be claimed by partner")),
      @ApiResponse(responseCode = "400", description = "Invalid LabId format"),
      @ApiResponse(responseCode = "403", description = "LabId Quota is exceeded by partner."),
      @ApiResponse(responseCode = "409", description = "LabId is already used by another partner."),
      @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
  @PostMapping("")
  public ResponseEntity<Void> claimLabId(
    @Valid @Pattern(regexp = "^[A-Za-z0-9]{1,64}$") @RequestHeader("X-CWA-PARTNER-ID") String partnerId,
    @Valid @org.springframework.web.bind.annotation.RequestBody LabIdClaimRequest claimRequest) {

    Optional<LabIdClaim> claim = labIdClaimService.getClaimEntity(claimRequest.getLabId());

    // LabId is claimed by another partner.
    if (claim.isPresent() && !claim.get().getPartnerId().equals(partnerId)) {
      return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .build();
    }

    int remainingClaims = labIdClaimService.getRemainingClaims(partnerId);

    // Quota exceeded
    if (claim.isEmpty() && remainingClaims <= 0) {
      return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .build();
    }

    // Everything is ok. Create the claim or just get it to update last used property.
    labIdClaimService.getClaim(partnerId, claimRequest.getLabId());

    // update remaining Claims
    remainingClaims = labIdClaimService.getRemainingClaims(partnerId);

    return ResponseEntity
      .status(HttpStatus.NO_CONTENT)
      .header(REMAINING_LAB_ID_HEADER, String.valueOf(remainingClaims))
      .build();
  }
}
