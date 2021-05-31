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

import app.coronawarn.dcc.model.DccResponse;
import app.coronawarn.dcc.model.DccUnexpectedError;
import app.coronawarn.dcc.model.RegistrationToken;
import app.coronawarn.dcc.model.UploadPublicKeyRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("external")
@RestController
@RequestMapping("/version/v1/dcc")
public class ExternalDccClaimController {

  @Operation(
    summary = "COVID-19 Test Result DCC Components",
    description = "Gets the components to build a Digital Covid Certificate with the result of COVID-19 Test.",
    tags = {"external"},
    parameters = {
      @Parameter(
        in = ParameterIn.HEADER,
        name = "cwa-fake",
        description = "Flag whether this request should be handled as fake request")
    },
    requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = RegistrationToken.class))),
    responses = {
      @ApiResponse(
        responseCode = "200",
        description = "DCC Components retrieved.",
        content = @Content(schema = @Schema(implementation = DccResponse.class))),
      @ApiResponse(responseCode = "202", description = "DCC is pending."),
      @ApiResponse(responseCode = "400", description = "Bad Request. (Invalid RegistrationToken format)"),
      @ApiResponse(
        responseCode = "404",
        description = "Registration Token does not exist/ is not registered at DCC-Server."),
      @ApiResponse(responseCode = "410", description = "DCC already cleaned up."),
      @ApiResponse(responseCode = "412", description = "TestResult not yet received."),
      @ApiResponse(
        responseCode = "500",
        description = "Internal Server Error",
        content = @Content(
          mediaType = MediaType.APPLICATION_JSON_VALUE,
          schema = @Schema(implementation = DccUnexpectedError.class)))
    })
  @PostMapping("")
  public ResponseEntity<Void> claimDcc(
    @RequestHeader("cwa-fake") String cwaFake,
    @org.springframework.web.bind.annotation.RequestBody UploadPublicKeyRequest requestBody) {

    return ResponseEntity.status(HttpStatus.OK).build();
  }
}
