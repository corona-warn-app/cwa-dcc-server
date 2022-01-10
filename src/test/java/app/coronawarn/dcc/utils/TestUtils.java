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
