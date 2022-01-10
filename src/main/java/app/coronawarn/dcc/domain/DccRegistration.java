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

package app.coronawarn.dcc.domain;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * This class represents the DccRegistration-entity.
 */
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "dcc_registration")
@Builder
@Getter
@Setter
public class DccRegistration implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  private Long id;

  @Version
  @Column(name = "version")
  private long version;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "hashed_guid")
  private String hashedGuid;

  @Column(name = "lab_id")
  private String labId;

  @Column(name = "partner_id")
  private String partnerId;

  @Column(name = "registration_token")
  private String registrationToken;

  @Column(name = "dcci")
  private String dcci;

  /**
   * PublicKey used to encrypt DEK, received by CWA-App.
   */
  @Column(name = "public_key")
  private String publicKey;

  /**
   * DEK encrpyted with PublicKey, received by LAB Server.
   */
  @Column(name = "encrypted_data_encryption_key")
  private String encryptedDataEncryptionKey;

  /**
   * Hash of unencrypted DCC Payload, received by LAB Server.
   */
  @Column(name = "dcc_hash")
  private String dccHash;

  /**
   * Encrypted DCC payload, received by LAB Server.
   */
  @Column(name = "dcc_encrypted_payload")
  private String dccEncryptedPayload;

  /**
   * DCC without payload, received by signing server.
   */
  @Column(name = "dcc")
  private String dcc;

  @Column(name = "error")
  @Enumerated(EnumType.STRING)
  private DccErrorReason error;

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  public void update() {
    updatedAt = LocalDateTime.now();
  }

}
