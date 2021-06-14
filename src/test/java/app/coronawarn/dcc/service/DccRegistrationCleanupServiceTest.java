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

package app.coronawarn.dcc.service;

import static app.coronawarn.dcc.utils.TestValues.dccHash;
import static app.coronawarn.dcc.utils.TestValues.encryptedDccBase64;
import static app.coronawarn.dcc.utils.TestValues.encryptedDekBase64;
import static app.coronawarn.dcc.utils.TestValues.labId;
import static app.coronawarn.dcc.utils.TestValues.partialDccBase64;
import static app.coronawarn.dcc.utils.TestValues.partnerId;
import static app.coronawarn.dcc.utils.TestValues.registrationTokenValue;
import static app.coronawarn.dcc.utils.TestValues.testId;

import app.coronawarn.dcc.domain.DccRegistration;
import app.coronawarn.dcc.repository.DccRegistrationRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DccRegistrationCleanupServiceTest {

  @Autowired
  DccRegistrationCleanupService dccRegistrationCleanupService;

  @Autowired
  DccRegistrationRepository dccRegistrationRepository;

  @Autowired
  EntityManagerFactory entityManagerFactory;

  @BeforeEach
  void setup() {
    dccRegistrationRepository.deleteAll();
  }

  @Test
  void testCleanupStage1() {
    DccRegistration dccRegistration = new DccRegistration(
      null,
      0,
      LocalDateTime.now(),
      LocalDateTime.now(),
      testId,
      labId,
      partnerId,
      registrationTokenValue,
      "dcci",
      "publicKey",
      encryptedDekBase64,
      dccHash,
      encryptedDccBase64,
      partialDccBase64,
      null);

    dccRegistration = dccRegistrationRepository.save(dccRegistration);

    // Manual Query to bypass OnUpdate method of entity
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    EntityTransaction transaction = entityManager.getTransaction();
    transaction.begin();

    Assertions.assertEquals(1, entityManager.createQuery("UPDATE DccRegistration d SET d.updatedAt=:date WHERE d.id=:id")
      .setParameter("date", LocalDateTime.now().minus(5, ChronoUnit.DAYS))
      .setParameter("id", dccRegistration.getId())
      .executeUpdate());

    transaction.commit();

    dccRegistrationCleanupService.cleanup();

    dccRegistration = dccRegistrationRepository.findById(dccRegistration.getId()).orElseThrow();

    Assertions.assertNull(dccRegistration.getPublicKey());
    Assertions.assertNull(dccRegistration.getEncryptedDataEncryptionKey());
    Assertions.assertNull(dccRegistration.getDccEncryptedPayload());
    Assertions.assertNull(dccRegistration.getDcc());
    Assertions.assertNull(dccRegistration.getError());
    Assertions.assertNull(dccRegistration.getHashedGuid());

    Assertions.assertEquals(registrationTokenValue, dccRegistration.getRegistrationToken());
    Assertions.assertEquals(labId, dccRegistration.getLabId());
    Assertions.assertEquals(partnerId, dccRegistration.getPartnerId());
    Assertions.assertEquals("dcci", dccRegistration.getDcci());
    Assertions.assertEquals(dccHash, dccRegistration.getDccHash());
  }

  @Test
  void testCleanupStage2() {
    DccRegistration dccRegistration = new DccRegistration(
      null,
      0,
      LocalDateTime.now(),
      LocalDateTime.now(),
      testId,
      labId,
      partnerId,
      registrationTokenValue,
      "dcci",
      "publicKey",
      encryptedDekBase64,
      dccHash,
      encryptedDccBase64,
      partialDccBase64,
      null);

    dccRegistration = dccRegistrationRepository.save(dccRegistration);
    dccRegistration.setCreatedAt(LocalDateTime.now().minus(22, ChronoUnit.DAYS));
    dccRegistration = dccRegistrationRepository.save(dccRegistration);

    // Manual Query to bypass OnUpdate method of entity
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    EntityTransaction transaction = entityManager.getTransaction();
    transaction.begin();

    Assertions.assertEquals(1, entityManager.createQuery("UPDATE DccRegistration d SET d.updatedAt=:date WHERE d.id=:id")
      .setParameter("date", LocalDateTime.now().minus(5, ChronoUnit.DAYS))
      .setParameter("id", dccRegistration.getId())
      .executeUpdate());

    transaction.commit();

    dccRegistrationCleanupService.cleanup();

    dccRegistration = dccRegistrationRepository.findById(dccRegistration.getId()).orElseThrow();

    Assertions.assertNull(dccRegistration.getPublicKey());
    Assertions.assertNull(dccRegistration.getEncryptedDataEncryptionKey());
    Assertions.assertNull(dccRegistration.getDccEncryptedPayload());
    Assertions.assertNull(dccRegistration.getDcc());
    Assertions.assertNull(dccRegistration.getError());
    Assertions.assertNull(dccRegistration.getHashedGuid());
    Assertions.assertNull(dccRegistration.getRegistrationToken());

    Assertions.assertEquals(labId, dccRegistration.getLabId());
    Assertions.assertEquals(partnerId, dccRegistration.getPartnerId());
    Assertions.assertEquals("dcci", dccRegistration.getDcci());
    Assertions.assertEquals(dccHash, dccRegistration.getDccHash());
  }

  @Test
  void testCleanupStage3() {
    DccRegistration dccRegistration = new DccRegistration(
      null,
      0,
      LocalDateTime.now(),
      LocalDateTime.now(),
      testId,
      labId,
      partnerId,
      registrationTokenValue,
      "dcci",
      "publicKey",
      encryptedDekBase64,
      dccHash,
      encryptedDccBase64,
      partialDccBase64,
      null);

    dccRegistration = dccRegistrationRepository.save(dccRegistration);

    // Manual Query to bypass OnUpdate method of entity
    EntityManager entityManager = entityManagerFactory.createEntityManager();
    EntityTransaction transaction = entityManager.getTransaction();
    transaction.begin();

    Assertions.assertEquals(1, entityManager.createQuery("UPDATE DccRegistration d SET d.updatedAt=:date WHERE d.id=:id")
      .setParameter("date", LocalDateTime.now().minus(181, ChronoUnit.DAYS))
      .setParameter("id", dccRegistration.getId())
      .executeUpdate());

    transaction.commit();

    dccRegistrationCleanupService.cleanup();

    Assertions.assertTrue(dccRegistrationRepository.findById(dccRegistration.getId()).isEmpty());
  }

}
