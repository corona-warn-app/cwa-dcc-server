package app.coronawarn.dcc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(
  description = "Information a lab receives when searching for PublicKeys"
)
@Data
public class LabPublicKeyInfo {

  @Schema(description = "Hashed GUID of the test.")
  private String testId;

  @Schema(description = "The DCCI of the to be created DCC.")
  private String dcci;

  @Schema(
    description = "The PublicKey to encrypt the Data Encryption Key with.",
    format = "Base64 encoded X509 SubjectPublicKeyInformation Object (RSA or EC Key)")
  private String publicKey;

}
