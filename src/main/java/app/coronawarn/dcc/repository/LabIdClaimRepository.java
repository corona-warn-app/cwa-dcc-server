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

package app.coronawarn.dcc.repository;

import app.coronawarn.dcc.domain.LabIdClaim;
import java.time.LocalDateTime;
import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Transactional
public interface LabIdClaimRepository extends JpaRepository<LabIdClaim, Long> {

  int countByPartnerId(String partnerId);

  Optional<LabIdClaim> findByLabId(String labId);

  @Modifying
  @Query("UPDATE LabIdClaim l SET l.lastUsed = current_timestamp WHERE l = :claim")
  void updateLastUsed(@Param("claim") LabIdClaim claim);

  @Modifying
  @Query("DELETE FROM LabIdClaim l WHERE l.lastUsed < :timestamp")
  int deleteClaimsOlderThan(@Param("timestamp") LocalDateTime timestamp);

}
