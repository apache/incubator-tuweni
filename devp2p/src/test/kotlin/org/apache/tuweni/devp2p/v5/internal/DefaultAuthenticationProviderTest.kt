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
package org.apache.tuweni.devp2p.v5.internal

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.dht.RoutingTable
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters
import org.apache.tuweni.devp2p.v5.misc.SessionKey
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetAddress

@ExtendWith(BouncyCastleExtension::class)
class DefaultAuthenticationProviderTest {

  private val providerKeyPair: SECP256K1.KeyPair = SECP256K1.KeyPair.random()
  private val providerEnr: Bytes = EthereumNodeRecord.toRLP(providerKeyPair, ip = InetAddress.getLocalHost())
  private val routingTable: RoutingTable = RoutingTable(providerEnr)
  private val authenticationProvider = DefaultAuthenticationProvider(providerKeyPair, routingTable)

  @Test
  fun authenticateReturnsValidAuthHeader() {
    val keyPair = SECP256K1.KeyPair.random()
    val nonce = Bytes.fromHexString("0x012715E4EFA2464F51BE49BBC40836E5816B3552249F8AC00AD1BBDB559E44E9")
    val authTag = Bytes.fromHexString("0x39BBC27C8CFA3735DF436AC6")
    val destEnr = EthereumNodeRecord.toRLP(keyPair, ip = InetAddress.getLocalHost())
    val params = HandshakeInitParameters(nonce, authTag, destEnr)

    val result = authenticationProvider.authenticate(params)

    assert(result.idNonce == nonce)
    assert(result.authTag == authTag)
    assert(result.authScheme == "gcm")
    assert(result.ephemeralPublicKey != providerKeyPair.publicKey().bytes())

    val destNodeId = Hash.sha2_256(destEnr).toHexString()

    assert(authenticationProvider.findSessionKey(destNodeId) != null)
  }

  @Test
  fun finalizeHandshakePersistsCreatedSessionKeys() {
    val keyPair = SECP256K1.KeyPair.random()
    val nonce = Bytes.fromHexString("0x012715E4EFA2464F51BE49BBC40836E5816B3552249F8AC00AD1BBDB559E44E9")
    val authTag = Bytes.fromHexString("0x39BBC27C8CFA3735DF436AC6")
    val destEnr = EthereumNodeRecord.toRLP(keyPair, ip = InetAddress.getLocalHost())
    val clientRoutingTable = RoutingTable(destEnr)
    val params = HandshakeInitParameters(nonce, authTag, providerEnr)
    val destNodeId = Hash.sha2_256(destEnr)

    val clientAuthProvider = DefaultAuthenticationProvider(keyPair, clientRoutingTable)

    val authHeader = clientAuthProvider.authenticate(params)

    authenticationProvider.finalizeHandshake(destNodeId, authHeader)

    assert(authenticationProvider.findSessionKey(destNodeId.toHexString()) != null)
  }

  @Test
  fun findSessionKeyRetrievesSessionKeyIfExists() {
    val result = authenticationProvider.findSessionKey(Bytes.random(32).toHexString())

    assert(result == null)
  }

  @Test
  fun setSessionKeyPersistsSessionKeyIfExists() {
    val nodeId = Bytes.random(32).toHexString()
    val bytes = Bytes.random(32)
    val sessionKey = SessionKey(bytes, bytes, bytes)

    authenticationProvider.setSessionKey(nodeId, sessionKey)

    val result = authenticationProvider.findSessionKey(nodeId)

    assert(result != null)
  }
}
