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

import app.coronawarn.dcc.exception.DccServerException;
import app.coronawarn.dcc.model.UploadPublicKeyRequest;
import app.coronawarn.dcc.service.DccRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.security.PublicKey;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("external")
@RestController
@RequestMapping("/version/v1/publicKey")
@RequiredArgsConstructor
public class ExternalPublicKeyController {

  private final DccRegistrationService dccRegistrationService;

  /**
   * Endpoint for inserting new DCC Registrations.
   */
  @Operation(
    summary = "Upload a Public Key",
    description = "Uploads a Public Key to a Registration Token from Verification Server to"
      + " generate Digital Covid Certificate data.",
    tags = {"external"},
    parameters = {
      @Parameter(
        in = ParameterIn.HEADER,
        name = "cwa-fake",
        description = "Flag whether this request should be handled as fake request")
    },
    requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = UploadPublicKeyRequest.class))),
    responses = {
      @ApiResponse(responseCode = "201", description = "Public Key uploaded and associated."),
      @ApiResponse(
        responseCode = "400",
        description = "Bad Request. (e.g. Wrong Format of RegistrationToken or PublicKey)."),
      @ApiResponse(
        responseCode = "403",
        description = "RegistrationToken is not allowed to issue a DCC (e.g. Token is issued for TeleTan)."),
      @ApiResponse(responseCode = "404", description = "RegistrationToken does not exists."),
      @ApiResponse(responseCode = "409", description = "RegistrationToken is already assigned with a PublicKey."),
      @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
  @PostMapping("")
  public ResponseEntity<Void> uploadPublicKey(
    @RequestHeader(name = "cwa-fake", required = false) String cwaFake,
    @org.springframework.web.bind.annotation.RequestBody UploadPublicKeyRequest requestBody) {

    PublicKey publicKey = dccRegistrationService.parsePublicKey(requestBody.getPublicKey());
    if (publicKey == null) {
      throw new DccServerException(HttpStatus.BAD_REQUEST, "Invalid Public Key");
    }

    try {
      dccRegistrationService.createDccRegistration(requestBody.getRegistrationToken(), publicKey);
    } catch (DccRegistrationService.DccRegistrationException e) {
      if (e.getReason()
        == DccRegistrationService.DccRegistrationException.Reason.INVALID_REGISTRATION_TOKEN_FORBIDDEN) {
        throw new DccServerException(HttpStatus.FORBIDDEN, "RegistrationToken is not allowed to issue a DCC");
      } else if (e.getReason()
        == DccRegistrationService.DccRegistrationException.Reason.INVALID_REGISTRATION_TOKEN_NOT_FOUND) {
        throw new DccServerException(HttpStatus.NOT_FOUND, "RegistrationToken does not exists.");
      } else if (e.getReason()
        == DccRegistrationService.DccRegistrationException.Reason.REGISTRATION_TOKEN_ALREADY_EXISTS) {
        throw new DccServerException(HttpStatus.CONFLICT, "RegistrationToken is already assigned with a PublicKey.");
      } else {
        throw new DccServerException(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
      }
    }

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
