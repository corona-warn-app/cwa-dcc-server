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


package app.coronawarn.dcc.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * This class represents the TestResult.
 *
 * @see <a href="https://github.com/corona-warn-app/cwa-testresult-server/blob/master/docs/architecture-overview.md#core-entities">Core Entities</a>
 */
@Schema(
  description = "The internal test result model."
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InternalTestResult {

  private int testResult;

  private String labId;

  private String testId;

  private long sc;

}
