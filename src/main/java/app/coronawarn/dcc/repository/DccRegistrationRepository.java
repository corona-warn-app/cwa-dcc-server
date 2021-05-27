/*-
 * ---license-start
 * Corona-Warn-App / cwa-dcc
 * ---
 * Copyright (C) 2020 - 2021 T-Systems International GmbH and all other contributors
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

import app.coronawarn.dcc.domain.DccRegistration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public interface DccRegistrationRepository extends JpaRepository<DccRegistration, Long> {

  Optional<DccRegistration> findByRegistrationToken(String registrationToken);

  List<DccRegistration> findByLabIdAndDccHashIsNull(String labId);

  Optional<DccRegistration> findByHashedGuid(String hashedGuid);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM DccRegistration d WHERE d.updatedAt < :threshold")
  int deleteEntityByUpdatedAtBefore(@Param("threshold") LocalDateTime threshold);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE DccRegistration d SET d.dcc = NULL, d.publicKey = NULL, d.encryptedDataEncryptionKey = NULL,"
    + " d.error = NULL, d.hashedGuid = NULL, d.dccEncryptedPayload = NULL"
    + " WHERE d.updatedAt < :threshold AND d.dcc IS NOT NULL")
  int removeDccDataByUpdatedAtBefore(@Param("threshold") LocalDateTime threshold);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("UPDATE DccRegistration d SET d.registrationToken = NULL"
    + " WHERE d.createdAt < :threshold AND d.registrationToken IS NOT NULL")
  int removeRegistrationTokenByCreatedAtBefore(@Param("threshold") LocalDateTime threshold);
}
