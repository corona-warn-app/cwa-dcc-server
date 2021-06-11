package app.coronawarn.dcc.utils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class TestUtils {

  public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("rsa");
    keyPairGenerator.initialize(3072);
    return keyPairGenerator.generateKeyPair();
  }

}
