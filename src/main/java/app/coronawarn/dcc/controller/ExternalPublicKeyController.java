package app.coronawarn.dcc.controller;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("external")
@RestController
@RequestMapping("/version/v1/publicKey")
public class ExternalPublicKeyController {

  @Operation(
    summary = "Upload a Public Key",
    description = "Uploads a Public Key to a Registration Token from Verification Server to"
      + " generate Digital Covid Certificate data.",
    tags = { "external" },
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
    @RequestHeader("cwa-fake") String cwaFake,
    @org.springframework.web.bind.annotation.RequestBody UploadPublicKeyRequest requestBody) {

    return ResponseEntity.status(HttpStatus.CREATED).build();
  }
}
