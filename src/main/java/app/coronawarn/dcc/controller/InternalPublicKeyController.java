package app.coronawarn.dcc.controller;

import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.model.LabPublicKeyInfo;
import app.coronawarn.dcc.model.UploadPublicKeyRequest;
import app.coronawarn.dcc.service.DccRegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("internal")
@RestController
@RequestMapping("/version/v1/publicKey")
@RequiredArgsConstructor
public class InternalPublicKeyController {

  private final DccRegistrationService dccRegistrationService;

  /**
   * Endpoint for inserting new DCC Registrations.
   */
  @Operation(
    summary = "Search Public Keys and Test Ids for given LabId",
    description = "Endpoint to search and download all PublicKeys which are assigned to a testId and the given lab Id.",
    tags = {"internal"},
    parameters = {
      @Parameter(name = "labId", in = ParameterIn.PATH, description = "ID of the laboratory to search for.")
    },
    requestBody = @RequestBody(content = @Content(schema = @Schema(implementation = UploadPublicKeyRequest.class))),
    responses = {
      @ApiResponse(responseCode = "200", description = "Public Key list returned."),
      @ApiResponse(responseCode = "500", description = "Internal Server Error")
    })
  @GetMapping("/search/{labId}")
  public ResponseEntity<List<LabPublicKeyInfo>> searchPublicKeys(
    @PathVariable("labId") String labId) {

    List<LabPublicKeyInfo> resultList = dccRegistrationService.findByLabId(labId).stream()
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
