/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.scuttlebutt.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import net.consensys.cava.crypto.sodium.Sodium;
import net.consensys.cava.scuttlebutt.Identity;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LocalIdentityTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void localIdentityInvalidPort() {
    assertThrows(IllegalArgumentException.class, () -> {
      new LocalIdentity("0.0.0.0", 450000, Identity.random());
    });
  }


  @Test
  void localIdentityInvalidIP() {
    assertThrows(IllegalArgumentException.class, () -> {
      new LocalIdentity("0.0.0.0a", 45000, Identity.random());
    });
  }

  @Test
  void localIdentityCanonicalForm() {
    Identity id = Identity.random();
    LocalIdentity localId = new LocalIdentity("0.0.0.0", 45000, id);
    assertEquals("net:0.0.0.0:45000~shs:" + id.publicKeyAsBase64String(), localId.toCanonicalForm());
    assertEquals(localId.toCanonicalForm(), localId.toString());
  }

  @Test
  void localIdentityFromString() {
    Identity id = Identity.random();
    LocalIdentity localId = new LocalIdentity("0.0.0.0", 45000, id);
    LocalIdentity fromString = LocalIdentity.fromString("net:0.0.0.0:45000~shs:" + id.publicKeyAsBase64String());
    assertEquals(fromString, localId);
  }

  @Test
  void malformedIdentity() {
    Identity id = Identity.random();
    assertNull(LocalIdentity.fromString("nt:0.0.0.0:45000~shs:" + id.publicKeyAsBase64String()));
    assertNull(LocalIdentity.fromString("net:0.0.0.0:45000~ss:" + id.publicKeyAsBase64String()));
    assertNull(LocalIdentity.fromString("nt:0.0.0.0:45000~shs:" + id.publicKeyAsBase64String().substring(12)));
  }
}
