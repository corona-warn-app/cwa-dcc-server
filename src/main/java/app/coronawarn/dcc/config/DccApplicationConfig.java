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

package app.coronawarn.dcc.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * This class and its nested subclasses are used to read in values from configuration file application.yml, which is
 * loaded via the '@EnableConfigurationProperties' annotation from SpringBootApplication main class.
 */
@Getter
@Setter
@ConfigurationProperties("cwa.dcc")
public class DccApplicationConfig {
  private Long initialFakeDelayMilliseconds;

  private Long fakeDelayMovingAverageSamples;

  private Cleanup cleanup = new Cleanup();
  private Request request = new Request();
  private String allowedClientCertificates;

  private MtlsConfiguration verificationServer = new MtlsConfiguration();
  private MtlsConfiguration signingApiServer = new MtlsConfiguration();

  private String dcciPrefix;

  private LabIdClaim labIdClaim = new LabIdClaim();

  @Getter
  @Setter
  public static class LabIdClaim {

    /**
     * Maximum age of a LabId Claim in days after last usage of the claimed LabId.
     */
    private int maximumAge = 30;

    /**
     * Maximum number of LabIds a partner is able to claim.
     */
    private int claimsPerPartner = 100;
  }

  /**
   * Entity Cleanup configuration.
   */
  @Getter
  @Setter
  public static class Cleanup {

    /**
     * Lifespan of DCC Data in days since last entity update.
     * (PublicKey, DEK, Encrypted Payload, DCC, ERROR, Hashed GUID)
     */
    private int dccData = 4;

    /**
     * Lifespan of RegistrationToken in days since entity creation.
     */
    private int registrationToken = 21;

    /**
     * Lifespan of whole entity in days since last entity update.
     */
    private int entity = 180;

    /**
     * Wait time between Cleanup Cycles in ms.
     */
    private int rate = 60000;
  }

  /**
   * Configure the requests with build property values and return the configured parameters.
   */
  @Getter
  @Setter
  public static class Request {

    private long sizelimit = 10000;
  }

  @Getter
  @Setter
  public static class MtlsConfiguration {

    private boolean enableMtls = true;

    private boolean verifyHostnames = true;

    private String keyStorePath;

    private char[] keyStorePassword;

    private String keyStoreAlias;

    private String trustStorePath;

    private char[] trustStorePassword;

    private String baseUrl;

    private ProxyConfig proxy = new ProxyConfig();

    private String apiKey;

    boolean connectionCloseWorkaround;

  }

  @Getter
  @Setter
  public static class ProxyConfig {

    private boolean enabled = false;
    private String host;
    private int port = -1;
  }
}
