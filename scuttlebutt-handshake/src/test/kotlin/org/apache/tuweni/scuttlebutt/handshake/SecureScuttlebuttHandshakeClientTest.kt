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
import org.apache.tuweni.crypto.sodium.SecretBox
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.crypto.sodium.Sodium
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.scuttlebutt.Identity.Companion.randomSECP256K1
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttHandshakeClient.Companion.create
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttHandshakeClient.Companion.fromInvite
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttHandshakeServer.Companion.create
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
internal class SecureScuttlebuttHandshakeClientTest {
  @Test
  fun initialMessage() {
    val serverLongTermKeyPair = Signature.KeyPair.random()
    val networkIdentifier = Bytes32.random()
    val client = create(Signature.KeyPair.random(), networkIdentifier, serverLongTermKeyPair.publicKey())
    val server = create(serverLongTermKeyPair, networkIdentifier)
    val initialMessage = client.createHello()
    server.readHello(initialMessage)
    client.readHello(server.createHello())
    Assertions.assertEquals(client.sharedSecret(), server.sharedSecret())
    Assertions.assertEquals(client.sharedSecret2(), server.sharedSecret2())
  }

  @Test
  fun initialMessageDifferentNetworkIdentifier() {
    val serverLongTermKeyPair = Signature.KeyPair.random()
    val client = create(Signature.KeyPair.random(), Bytes32.random(), serverLongTermKeyPair.publicKey())
    val server = create(serverLongTermKeyPair, Bytes32.random())
    val initialMessage = client.createHello()
    Assertions.assertThrows(HandshakeException::class.java) {
      server.readHello(
        initialMessage
      )
    }
  }

  @Test
  fun identityMessageExchangedFixedParameters() {
    val clientLongTermKeyPair = Signature.KeyPair
      .forSecretKey(
        Signature.SecretKey
          .fromBytes(
            Bytes
              .fromHexString(
                "0x4936543930b3d7de00ecb78952b9f6579b40b73b89512c108b35815e7b35856e9464f3d5d26fa22b3f6604cac7e41c24855fd756a326d7a22995c92bbbad1049"
              )
          )
      )
    val serverLongTermKeyPair = Signature.KeyPair
      .forSecretKey(
        Signature.SecretKey
          .fromBytes(
            Bytes
              .fromHexString(
                "0x05b578a14b4fef8386ffd509d6241a4a3a0a1d560603dacb6f13df01ed8a63221db3ee42b856345dde400e2f32014aed1a83c7d77ac573cce9bd32412631d607"
              )
          )
      )
    val networkIdentifier = Bytes32.fromHexString("0x346105b79c062220c598f95941ab5ea05e7e8d31af9d2f63d46a2326a1e43ac5")
    val client = create(clientLongTermKeyPair, networkIdentifier, serverLongTermKeyPair.publicKey())
    val server = create(serverLongTermKeyPair, networkIdentifier)
    val initialMessage = client.createHello()
    server.readHello(initialMessage)
    client.readHello(server.createHello())
    server.readIdentityMessage(client.createIdentityMessage())
    Assertions.assertEquals(clientLongTermKeyPair.publicKey(), server.clientLongTermPublicKey())
  }

  @Test
  fun identityMessageExchanged() {
    val clientLongTermKeyPair = Signature.KeyPair.random()
    val serverLongTermKeyPair = Signature.KeyPair.random()
    val networkIdentifier = Bytes32.random()
    val client = create(clientLongTermKeyPair, networkIdentifier, serverLongTermKeyPair.publicKey())
    val server = create(serverLongTermKeyPair, networkIdentifier)
    val initialMessage = client.createHello()
    server.readHello(initialMessage)
    client.readHello(server.createHello())
    server.readIdentityMessage(client.createIdentityMessage())
    Assertions.assertEquals(clientLongTermKeyPair.publicKey(), server.clientLongTermPublicKey())
  }

  @Test
  fun acceptMessageExchanged() {
    val clientLongTermKeyPair = Signature.KeyPair.random()
    val serverLongTermKeyPair = Signature.KeyPair.random()
    val networkIdentifier = Bytes32.random()
    val client = create(clientLongTermKeyPair, networkIdentifier, serverLongTermKeyPair.publicKey())
    val server = create(serverLongTermKeyPair, networkIdentifier)
    val initialMessage = client.createHello()
    server.readHello(initialMessage)
    client.readHello(server.createHello())
    server.readIdentityMessage(client.createIdentityMessage())
    client.readAcceptMessage(server.createAcceptMessage())
    Assertions.assertEquals(client.sharedSecret(), server.sharedSecret())
    Assertions.assertEquals(client.sharedSecret2(), server.sharedSecret2())
    Assertions.assertEquals(client.sharedSecret3(), server.sharedSecret3())
  }

  @Test
  fun finalSecretsUsed() {
    val clientLongTermKeyPair = Signature.KeyPair.random()
    val serverLongTermKeyPair = Signature.KeyPair.random()
    val networkIdentifier = Bytes32.random()
    val client = create(clientLongTermKeyPair, networkIdentifier, serverLongTermKeyPair.publicKey())
    val server = create(serverLongTermKeyPair, networkIdentifier)
    val initialMessage = client.createHello()
    server.readHello(initialMessage)
    client.readHello(server.createHello())
    server.readIdentityMessage(client.createIdentityMessage())
    client.readAcceptMessage(server.createAcceptMessage())
    run {
      val encrypted = SecretBox
        .encrypt(
          Bytes.fromHexString("deadbeef"),
          SecretBox.Key.fromHash(server.serverToClientSecretBoxKey()),
          SecretBox.Nonce.fromBytes(server.serverToClientNonce())
        )
      val decrypted = SecretBox
        .decrypt(
          encrypted,
          SecretBox.Key.fromHash(client.serverToClientSecretBoxKey()),
          SecretBox.Nonce.fromBytes(client.serverToClientNonce())
        )
      Assertions.assertEquals(
        Bytes.fromHexString("deadbeef"),
        decrypted
      )
    }
    run {
      val encrypted = SecretBox
        .encrypt(
          Bytes.fromHexString("deadbeef"),
          SecretBox.Key.fromHash(client.clientToServerSecretBoxKey()),
          SecretBox.Nonce.fromBytes(client.clientToServerNonce())
        )
      val decrypted = SecretBox
        .decrypt(
          encrypted,
          SecretBox.Key.fromHash(server.clientToServerSecretBoxKey()),
          SecretBox.Nonce.fromBytes(server.clientToServerNonce())
        )
      Assertions.assertEquals(
        Bytes.fromHexString("deadbeef"),
        decrypted
      )
    }
  }

  @Test
  fun fromInviteWrongCurve() {
    Assertions.assertThrows(
      IllegalArgumentException::class.java
    ) {
      fromInvite(
        Bytes32.random(),
        Invite(
          "localhost",
          30303,
          randomSECP256K1(),
          Signature.Seed.random()
        )
      )
    }
  }

  companion object {
    @BeforeAll
    fun checkAvailable() {
      Assumptions.assumeTrue(Sodium.isAvailable(), "Sodium native library is not available")
    }
  }
}
