package app.coronawarn.dcc.utils;

import com.upokecenter.cbor.CBORObject;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class TestUtils {

  public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("rsa");
    keyPairGenerator.initialize(3072);
    return keyPairGenerator.generateKeyPair();
  }

  public static byte[] generatePartialDcc() {
    CBORObject unprotectedHeader = CBORObject.NewMap();
    unprotectedHeader.set(4, CBORObject.FromObject("unprotected Header".getBytes()));

    CBORObject cose = CBORObject.NewArray();

    cose
      .Add(CBORObject.FromObject("protected Header".getBytes()))
      .Add(unprotectedHeader)
      .Add(CBORObject.FromObject("payload".getBytes()))
      .Add(CBORObject.FromObject("signature".getBytes()));

    return cose.EncodeToBytes();
}

}
