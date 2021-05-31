/*
 * Corona-Warn-App / cwa-dcc
 *
 * (C) 2020, T-Systems International GmbH
 *
 * Deutsche Telekom AG and all other contributors /
 * copyright owners license this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package app.coronawarn.dcc.client;

import app.coronawarn.dcc.model.InternalTestResult;
import app.coronawarn.dcc.model.RegistrationToken;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * This class represents the Verification Server Feign client.
 */
@FeignClient(
  name = "verificationServerClient",
  url = "${cwa.dcc.verification-server.base-url}",
  configuration = VerificationServerClientConfig.class)
public interface VerificationServerClient {

  /**
   * This method gets a testResult from the LabServer.
   *
   * @param registrationToken for TestResult
   * @return TestResult from server
   */
  @PostMapping(value = "/version/v1/testresult",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE
  )
  InternalTestResult result(RegistrationToken registrationToken);
}
