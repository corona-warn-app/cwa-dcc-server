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
import app.coronawarn.dcc.model.DccUploadRequest;
import app.coronawarn.dcc.model.DccUploadResponse;
import app.coronawarn.dcc.service.DccRegistrationService;
import app.coronawarn.dcc.service.DccService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

  /**
   * Endpoint for inserting new DCC Registrations.
   */
  @Operation(
    summary = "Upload a DCC for a test.",
    description = "Endpoint to upload components to build the DCC.",
    tags = {"internal"},
    parameters = {
      @Parameter(name = "testId", in = ParameterIn.PATH, description = "ID of the test (hashed GUID).")
    },
    requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = DccUploadRequest.class))),
    responses = {
      @ApiResponse(
        responseCode = "201",
        description = "DCC created",
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = DccUploadResponse.class))),
      @ApiResponse(
        responseCode = "200",
        description = "DCC updated",
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = DccUploadResponse.class))),
      @ApiResponse(responseCode = "400", description = "Invalid Data format"),
      @ApiResponse(responseCode = "404", description = "Test does not exists"),
      @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
  @GetMapping("/{testId}/dcc")
  public ResponseEntity<DccUploadResponse> uploadDcc(
    @Valid @Pattern(regexp = "^[XxA-Fa-f0-9]([A-Fa-f0-9]{63})$") @PathVariable("testId") String testId,
    @Valid @org.springframework.web.bind.annotation.RequestBody DccUploadRequest uploadRequest) {

    DccRegistration dccRegistration = dccRegistrationService.findByHashedGuid(testId).orElseThrow(
      () -> new DccServerException(HttpStatus.NOT_FOUND, "Test does not exists"));

    // Check whether this DCC will be updated or newly created.
    final boolean updated = dccRegistration.getDcc() != null;

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
      uploadRequest.getDataEncryptionKey());

    try {
      dccRegistration = dccService.sign(dccRegistration);
    } catch (DccService.DccGenerateException e) {
      throw new DccServerException(HttpStatus.INTERNAL_SERVER_ERROR, e.getReason().toString());
    }

    DccUploadResponse dccUploadResponse = new DccUploadResponse(dccRegistration.getDcc());

    return ResponseEntity
      .status(updated ? HttpStatus.OK : HttpStatus.CREATED)
      .body(dccUploadResponse);
  }
}
