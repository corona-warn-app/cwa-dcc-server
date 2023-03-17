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

package app.coronawarn.dcc.service;

import app.coronawarn.dcc.config.DccApplicationConfig;
import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DcciGeneratorService {
  private final DccApplicationConfig config;

  private static final String CODE_POINTS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789/:";

  /**
   * Check if dcci prefix contains character suitable for checksum calculation.
   */
  @PostConstruct
  public void checkPrefix() {
    String dcciPrefix = config.getDcciPrefix();

    if (dcciPrefix != null) {
      for (int i = 0; i < dcciPrefix.length(); i++) {
        if (CODE_POINTS.indexOf(dcciPrefix.charAt(i)) < 0) {
          throw new IllegalArgumentException(String.format(
            "configured DCCI prefix %s contains invalid character %s only following are supported %s",
            dcciPrefix, dcciPrefix.charAt(i), CODE_POINTS));
        }
      }
    }
  }

  /**
   * Generates a new DCCI.
   *
   * @return DCCI as String
   */
  public String newDcci() {
    StringBuilder sb = new StringBuilder();

    return sb.append(config.getDcciPrefix())
      .append(':')
      .append(encodeDcci(UUID.randomUUID()))
      .append(generateCheckCharacter(sb.toString()))
      .toString();
  }

  private static String encodeDcci(UUID uuid) {
    ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
    bb.putLong(uuid.getMostSignificantBits());
    bb.putLong(uuid.getLeastSignificantBits());
    BigInteger bint = new BigInteger(1, bb.array());
    int radix = 10 + ('Z' - 'A');
    return bint.toString(radix).toUpperCase();
  }

  // see https://en.wikipedia.org/wiki/Luhn_mod_N_algorithm
  private char generateCheckCharacter(String input) {
    int factor = 2;
    int sum = 0;
    int n = CODE_POINTS.length();

    // Starting from the right and working leftwards is easier since
    // the initial "factor" will always be "2".
    for (int i = input.length() - 1; i >= 0; i--) {
      int codePoint = codePointFromCharacter(input.charAt(i));
      int addend = factor * codePoint;

      // Alternate the "factor" that each "codePoint" is multiplied by
      factor = (factor == 2) ? 1 : 2;

      // Sum the digits of the "addend" as expressed in base "n"
      addend = (addend / n) + (addend % n);
      sum += addend;
    }

    // Calculate the number that must be added to the "sum"
    // to make it divisible by "n".
    int remainder = sum % n;
    int checkCodePoint = (n - remainder) % n;

    return characterFromCodePoint(checkCodePoint);
  }

  private char characterFromCodePoint(int checkCodePoint) {
    return CODE_POINTS.charAt(checkCodePoint);
  }

  private int codePointFromCharacter(char charAt) {
    int codePoint = CODE_POINTS.indexOf(charAt);
    if (codePoint < 0) {
      throw new IllegalArgumentException("unsupported character for checksum: " + charAt);
    }
    return codePoint;
  }
}
