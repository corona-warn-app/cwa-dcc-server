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

import app.coronawarn.dcc.domain.DccErrorReason;
import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.exception.DccServerException;
import app.coronawarn.dcc.model.DccUnexpectedError;
import app.coronawarn.dcc.model.DccUploadRequest;
import app.coronawarn.dcc.model.DccUploadResponse;
import app.coronawarn.dcc.service.DccRegistrationService;
import app.coronawarn.dcc.service.DccService;
import app.coronawarn.dcc.service.LabIdClaimService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.Base64;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("internal")
@RestController
@RequestMapping("/version/v1/test")
@RequiredArgsConstructor
@Validated
public class InternalDccController {

  private final DccRegistrationService dccRegistrationService;

  private final DccService dccService;

  private final LabIdClaimService labIdClaimService;

  /**
   * Endpoint for inserting new DCC Registrations.
   */
  @Operation(
    summary = "Upload a DCC for a test.",
    description = "Endpoint to upload components to build the DCC.",
    tags = {"internal"},
    parameters = {
      @Parameter(name = "testId", in = ParameterIn.PATH, description = "ID of the test (hashed GUID)."),
      @Parameter(name = "X-CWA-PARTNER-ID", in = ParameterIn.HEADER, description = "PartnerID. This needs only to be"
        + " set if DCC-Server is contacted without DCC-Proxy in between.")
    },
    requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DccUploadRequest.class))),
    responses = {
      @ApiResponse(
        responseCode = "201",
        description = "DCC created",
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = DccUploadResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid Data format"),
      @ApiResponse(responseCode = "403", description = "LabId is not or cannot be assigned to Partner."),
      @ApiResponse(responseCode = "404", description = "Test does not exists"),
      @ApiResponse(responseCode = "409", description = "DCC already exists"),
      @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
  @PostMapping("/{testId}/dcc")
  public ResponseEntity<?> uploadDcc(
    @Valid @Pattern(regexp = "^[XxA-Fa-f0-9]([A-Fa-f0-9]{63})$") @PathVariable("testId") String testId,
    @Valid @Pattern(regexp = "^[A-Za-z0-9]{1,64}$") @RequestHeader("X-CWA-PARTNER-ID") String partnerId,
    @Valid @org.springframework.web.bind.annotation.RequestBody DccUploadRequest uploadRequest) {

    DccRegistration dccRegistration = dccRegistrationService.findByHashedGuid(testId).orElseThrow(
      () -> new DccServerException(HttpStatus.NOT_FOUND, "Test does not exists"));

    if (!labIdClaimService.getClaim(partnerId, dccRegistration.getLabId())) {
      throw new DccServerException(HttpStatus.FORBIDDEN, "Failed to claim LabId");
    }

    if (dccRegistration.getDccHash() != null) {
      throw new DccServerException(HttpStatus.CONFLICT, "DCC already exists");
    }

    try {
      Base64.getDecoder().decode(uploadRequest.getDataEncryptionKey());
      Base64.getDecoder().decode(uploadRequest.getEncryptedDcc());
    } catch (IllegalArgumentException e) {
      dccRegistrationService.setError(dccRegistration, DccErrorReason.LAB_INVALID_RESPONSE);

      throw new DccServerException(HttpStatus.BAD_REQUEST, "Invalid Base64 in DEK or encrypted DCC.");
    }

    dccRegistrationService.updateDccRegistration(
      dccRegistration,
      uploadRequest.getDccHash(),
      uploadRequest.getEncryptedDcc(),
      uploadRequest.getDataEncryptionKey(),
      partnerId);

    try {
      dccRegistration = dccService.sign(dccRegistration);
    } catch (DccService.DccGenerateException e) {

      // Delete DCC information if signing failed
      dccRegistrationService.updateDccRegistration(
        dccRegistration,
        null,
        null,
        null,
        partnerId);

      return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new DccUnexpectedError(dccRegistration.getError()));
    }

    return ResponseEntity.ok(new DccUploadResponse(dccRegistration.getDcc()));
  }
}
