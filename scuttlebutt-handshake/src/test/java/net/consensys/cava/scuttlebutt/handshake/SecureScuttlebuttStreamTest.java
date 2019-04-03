/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.handshake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.sodium.SHA256Hash;
import org.apache.tuweni.crypto.sodium.Sodium;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SecureScuttlebuttStreamTest {

  @BeforeAll
  static void checkAvailable() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void streamExchange() {
    SHA256Hash.Hash clientToServerKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()));
    Bytes clientToServerNonce = Bytes.random(24);
    SHA256Hash.Hash serverToClientKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()));
    Bytes serverToClientNonce = Bytes.random(24);
    SecureScuttlebuttStream clientToServer =
        new SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce);
    SecureScuttlebuttStream serverToClient =
        new SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce);

    Bytes encrypted = clientToServer.sendToServer(Bytes.fromHexString("deadbeef"));
    assertEquals(Bytes.fromHexString("deadbeef").size() + 34, encrypted.size());

    Bytes decrypted = serverToClient.readFromClient(encrypted);
    assertEquals(Bytes.fromHexString("deadbeef"), decrypted);

    Bytes response = serverToClient.sendToClient(Bytes.fromHexString("deadbeef"));
    assertEquals(Bytes.fromHexString("deadbeef").size() + 34, response.size());

    Bytes responseDecrypted = clientToServer.readFromServer(response);
    assertEquals(Bytes.fromHexString("deadbeef"), responseDecrypted);
  }

  @Test
  void longMessage() {
    SHA256Hash.Hash clientToServerKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()));
    Bytes clientToServerNonce = Bytes.random(24);
    SHA256Hash.Hash serverToClientKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()));
    Bytes serverToClientNonce = Bytes.random(24);
    SecureScuttlebuttStream clientToServer =
        new SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce);
    SecureScuttlebuttStream serverToClient =
        new SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce);

    Bytes payload = Bytes.random(5128);
    Bytes encrypted = clientToServer.sendToServer(payload);
    assertEquals(5128 + 34 + 34, encrypted.size());
    Bytes decrypted = serverToClient.readFromClient(encrypted);
    assertEquals(payload, decrypted);

    Bytes encrypted2 = serverToClient.sendToClient(payload);
    assertEquals(5128 + 34 + 34, encrypted2.size());

    Bytes decrypted2 = clientToServer.readFromServer(encrypted2);
    assertEquals(payload, decrypted2);
  }

  @Test
  void multipleMessages() {
    SHA256Hash.Hash clientToServerKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()));
    Bytes clientToServerNonce = Bytes.random(24);
    SHA256Hash.Hash serverToClientKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()));
    Bytes serverToClientNonce = Bytes.random(24);
    SecureScuttlebuttStream clientToServer =
        new SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce);
    SecureScuttlebuttStream serverToClient =
        new SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce);

    for (int i = 0; i < 10; i++) {
      Bytes encrypted = clientToServer.sendToServer(Bytes.fromHexString("deadbeef"));
      assertEquals(Bytes.fromHexString("deadbeef").size() + 34, encrypted.size());

      Bytes decrypted = serverToClient.readFromClient(encrypted);
      assertEquals(Bytes.fromHexString("deadbeef"), decrypted);

      Bytes response = serverToClient.sendToClient(Bytes.fromHexString("deadbeef"));
      assertEquals(Bytes.fromHexString("deadbeef").size() + 34, response.size());

      Bytes responseDecrypted = clientToServer.readFromServer(response);
      assertEquals(Bytes.fromHexString("deadbeef"), responseDecrypted);
    }
  }

  @Test
  void chunkedMessages() {
    SHA256Hash.Hash clientToServerKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()));
    Bytes clientToServerNonce = Bytes.random(24);
    SHA256Hash.Hash serverToClientKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()));
    Bytes serverToClientNonce = Bytes.random(24);
    SecureScuttlebuttStream clientToServer =
        new SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce);
    SecureScuttlebuttStream serverToClient =
        new SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce);

    Bytes payload = Bytes.random(5128);
    Bytes encrypted = clientToServer.sendToServer(payload);
    assertEquals(5128 + 34 + 34, encrypted.size());
    serverToClient.readFromClient(encrypted.slice(0, 20));
    serverToClient.readFromClient(encrypted.slice(20, 400));
    Bytes part1 = serverToClient.readFromClient(encrypted.slice(420, 4000));
    Bytes decrypted = serverToClient.readFromClient(encrypted.slice(4420));
    assertEquals(payload, Bytes.concatenate(part1, decrypted));

    Bytes encrypted2 = serverToClient.sendToClient(payload);
    assertEquals(5128 + 34 + 34, encrypted2.size());
    clientToServer.readFromServer(encrypted2.slice(0, 20));
    clientToServer.readFromServer(encrypted2.slice(20, 400));
    Bytes part2 = clientToServer.readFromServer(encrypted2.slice(420, 4000));
    Bytes decrypted2 = clientToServer.readFromServer(encrypted2.slice(4420));
    assertEquals(payload, Bytes.concatenate(part2, decrypted2));
  }

  @Test
  void testGoodbyeCheck() {
    assertFalse(SecureScuttlebuttStreamServer.isGoodbye(Bytes.wrap(new byte[17])));
    assertFalse(SecureScuttlebuttStreamServer.isGoodbye(Bytes.wrap(new byte[19])));
    assertFalse(SecureScuttlebuttStreamServer.isGoodbye(Bytes.random(18)));
  }

  @Test
  void sendingGoodbyes() {
    SHA256Hash.Hash clientToServerKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()));
    Bytes clientToServerNonce = Bytes.random(24);
    SHA256Hash.Hash serverToClientKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()));
    Bytes serverToClientNonce = Bytes.random(24);
    SecureScuttlebuttStream clientToServer =
        new SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce);
    SecureScuttlebuttStream serverToClient =
        new SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce);
    Bytes message = clientToServer.sendGoodbyeToServer();
    assertTrue(SecureScuttlebuttStreamServer.isGoodbye(serverToClient.readFromClient(message)));
    assertTrue(
        SecureScuttlebuttStreamServer.isGoodbye(clientToServer.readFromServer(serverToClient.sendGoodbyeToClient())));
  }
}
