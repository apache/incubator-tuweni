/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.handshake

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.sodium.SHA256Hash
import org.apache.tuweni.crypto.sodium.Sodium
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class SecureScuttlebuttStreamTest {
  @Test
  fun streamExchange() {
    val clientToServerKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()))
    val clientToServerNonce = Bytes.random(24)
    val serverToClientKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()))
    val serverToClientNonce = Bytes.random(24)
    val clientToServer =
      SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce)
    val serverToClient =
      SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce)
    val encrypted = clientToServer.sendToServer(Bytes.fromHexString("deadbeef"))
    Assertions.assertEquals(Bytes.fromHexString("deadbeef").size() + 34, encrypted.size())
    val decrypted = serverToClient.readFromClient(encrypted)
    Assertions.assertEquals(Bytes.fromHexString("deadbeef"), decrypted)
    val response = serverToClient.sendToClient(Bytes.fromHexString("deadbeef"))
    Assertions.assertEquals(Bytes.fromHexString("deadbeef").size() + 34, response.size())
    val responseDecrypted = clientToServer.readFromServer(response)
    Assertions.assertEquals(Bytes.fromHexString("deadbeef"), responseDecrypted)
  }

  @Test
  fun longMessage() {
    val clientToServerKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()))
    val clientToServerNonce = Bytes.random(24)
    val serverToClientKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()))
    val serverToClientNonce = Bytes.random(24)
    val clientToServer =
      SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce)
    val serverToClient =
      SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce)
    val payload = Bytes.random(5128)
    val encrypted = clientToServer.sendToServer(payload)
    Assertions.assertEquals(5128 + 34 + 34, encrypted.size())
    val decrypted = serverToClient.readFromClient(encrypted)
    Assertions.assertEquals(payload, decrypted)
    val encrypted2 = serverToClient.sendToClient(payload)
    Assertions.assertEquals(5128 + 34 + 34, encrypted2.size())
    val decrypted2 = clientToServer.readFromServer(encrypted2)
    Assertions.assertEquals(payload, decrypted2)
  }

  @Test
  fun multipleMessages() {
    val clientToServerKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()))
    val clientToServerNonce = Bytes.random(24)
    val serverToClientKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()))
    val serverToClientNonce = Bytes.random(24)
    val clientToServer =
      SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce)
    val serverToClient =
      SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce)
    for (i in 0..9) {
      val encrypted = clientToServer.sendToServer(Bytes.fromHexString("deadbeef"))
      Assertions.assertEquals(Bytes.fromHexString("deadbeef").size() + 34, encrypted.size())
      val decrypted = serverToClient.readFromClient(encrypted)
      Assertions.assertEquals(Bytes.fromHexString("deadbeef"), decrypted)
      val response = serverToClient.sendToClient(Bytes.fromHexString("deadbeef"))
      Assertions.assertEquals(Bytes.fromHexString("deadbeef").size() + 34, response.size())
      val responseDecrypted = clientToServer.readFromServer(response)
      Assertions.assertEquals(Bytes.fromHexString("deadbeef"), responseDecrypted)
    }
  }

  @Test
  fun chunkedMessages() {
    val clientToServerKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()))
    val clientToServerNonce = Bytes.random(24)
    val serverToClientKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()))
    val serverToClientNonce = Bytes.random(24)
    val clientToServer =
      SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce)
    val serverToClient =
      SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce)
    val payload = Bytes.random(5128)
    val encrypted = clientToServer.sendToServer(payload)
    Assertions.assertEquals(5128 + 34 + 34, encrypted.size())
    serverToClient.readFromClient(encrypted.slice(0, 20))
    serverToClient.readFromClient(encrypted.slice(20, 400))
    val part1 = serverToClient.readFromClient(encrypted.slice(420, 4000))
    val decrypted = serverToClient.readFromClient(encrypted.slice(4420))
    Assertions.assertEquals(payload, Bytes.concatenate(part1, decrypted))
    val encrypted2 = serverToClient.sendToClient(payload)
    Assertions.assertEquals(5128 + 34 + 34, encrypted2.size())
    clientToServer.readFromServer(encrypted2.slice(0, 20))
    clientToServer.readFromServer(encrypted2.slice(20, 400))
    val part2 = clientToServer.readFromServer(encrypted2.slice(420, 4000))
    val decrypted2 = clientToServer.readFromServer(encrypted2.slice(4420))
    Assertions.assertEquals(payload, Bytes.concatenate(part2, decrypted2))
  }

  @Test
  fun testGoodbyeCheck() {
    assertFalse(SecureScuttlebuttStreamServer.isGoodbye(Bytes.wrap(ByteArray(17))))
    assertFalse(SecureScuttlebuttStreamServer.isGoodbye(Bytes.wrap(ByteArray(19))))
    assertFalse(SecureScuttlebuttStreamServer.isGoodbye(Bytes.random(18)))
  }

  @Test
  fun sendingGoodbyes() {
    val clientToServerKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()))
    val clientToServerNonce = Bytes.random(24)
    val serverToClientKey = SHA256Hash.hash(SHA256Hash.Input.fromBytes(Bytes32.random()))
    val serverToClientNonce = Bytes.random(24)
    val clientToServer =
      SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce)
    val serverToClient =
      SecureScuttlebuttStream(clientToServerKey, clientToServerNonce, serverToClientKey, serverToClientNonce)
    val message = clientToServer.sendGoodbyeToServer()
    assertTrue(SecureScuttlebuttStreamServer.isGoodbye(serverToClient.readFromClient(message)))
    assertTrue(
      SecureScuttlebuttStreamServer.isGoodbye(clientToServer.readFromServer(serverToClient.sendGoodbyeToClient()))
    )
  }

  companion object {
    @BeforeAll
    fun checkAvailable() {
      Assumptions.assumeTrue(Sodium.isAvailable(), "Sodium native library is not available")
    }
  }
}
