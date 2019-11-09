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

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.AuthenticationProvider
import org.apache.tuweni.devp2p.v5.storage.RoutingTable
import org.apache.tuweni.devp2p.v5.encrypt.AES128GCM
import org.apache.tuweni.devp2p.v5.encrypt.SessionKeyGenerator
import org.apache.tuweni.devp2p.v5.misc.AuthHeader
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters
import org.apache.tuweni.devp2p.v5.misc.SessionKey
import org.apache.tuweni.rlp.RLP
import java.util.concurrent.TimeUnit

class DefaultAuthenticationProvider(
  private val keyPair: SECP256K1.KeyPair,
  private val routingTable: RoutingTable
) : AuthenticationProvider {

  private val sessionKeys: Cache<String, SessionKey> = CacheBuilder
    .newBuilder()
    .expireAfterWrite(SESSION_KEY_EXPIRATION, TimeUnit.MINUTES)
    .build()
  private val nodeId: Bytes = Hash.sha2_256(routingTable.getSelfEnr())

  @Synchronized
  override fun authenticate(handshakeParams: HandshakeInitParameters): AuthHeader {
    // Generate ephemeral key pair
    val ephemeralKeyPair = SECP256K1.KeyPair.random()
    val ephemeralKey = ephemeralKeyPair.secretKey()

    val destEnr = EthereumNodeRecord.fromRLP(handshakeParams.destEnr)
    val destNodeId = Hash.sha2_256(handshakeParams.destEnr)

    // Perform agreement
    val secret = SECP256K1.calculateKeyAgreement(ephemeralKey, destEnr.publicKey())

    // Derive keys
    val sessionKey = SessionKeyGenerator.generate(nodeId, destNodeId, secret, handshakeParams.idNonce)

    sessionKeys.put(destNodeId.toHexString(), sessionKey)

    val signature = sign(keyPair, handshakeParams)

    return generateAuthHeader(
      routingTable.getSelfEnr(),
      signature,
      handshakeParams,
      sessionKey.authRespKey,
      ephemeralKeyPair.publicKey()
    )
  }

  @Synchronized
  override fun findSessionKey(nodeId: String): SessionKey? {
    return sessionKeys.getIfPresent(nodeId)
  }

  @Synchronized
  override fun setSessionKey(nodeId: String, sessionKey: SessionKey) {
    sessionKeys.put(nodeId, sessionKey)
  }

  @Synchronized
  override fun finalizeHandshake(senderNodeId: Bytes, authHeader: AuthHeader) {
    val ephemeralPublicKey = SECP256K1.PublicKey.fromBytes(authHeader.ephemeralPublicKey)
    val secret = SECP256K1.calculateKeyAgreement(keyPair.secretKey(), ephemeralPublicKey)

    val sessionKey = SessionKeyGenerator.generate(senderNodeId, nodeId, secret, authHeader.idNonce)

    val decryptedAuthResponse = AES128GCM.decrypt(authHeader.authResponse, sessionKey.authRespKey, Bytes.EMPTY)
    RLP.decodeList(Bytes.wrap(decryptedAuthResponse)) { reader ->
      reader.skipNext()
      val signatureBytes = reader.readValue()
      val enrRLP = reader.readValue()
      val enr = EthereumNodeRecord.fromRLP(enrRLP)
      val publicKey = enr.publicKey()
      val signatureVerified = verifySignature(signatureBytes, authHeader.idNonce, publicKey)
      if (!signatureVerified) {
        throw IllegalArgumentException("Signature is not verified")
      }
      sessionKeys.put(senderNodeId.toHexString(), sessionKey)
      routingTable.add(enrRLP)
    }
  }

  private fun sign(keyPair: SECP256K1.KeyPair, params: HandshakeInitParameters): SECP256K1.Signature {
    val signValue = Bytes.wrap(DISCOVERY_ID_NONCE, params.idNonce)
    val hashedSignValue = Hash.sha2_256(signValue)
    return SECP256K1.sign(hashedSignValue, keyPair)
  }

  private fun verifySignature(signatureBytes: Bytes, idNonce: Bytes, publicKey: SECP256K1.PublicKey): Boolean {
    val signature = SECP256K1.Signature.fromBytes(signatureBytes)
    val signValue = Bytes.wrap(DISCOVERY_ID_NONCE, idNonce)
    val hashedSignValue = Hash.sha2_256(signValue)
    return SECP256K1.verify(hashedSignValue, signature, publicKey)
  }

  private fun generateAuthHeader(
    enr: Bytes,
    signature: SECP256K1.Signature,
    params: HandshakeInitParameters,
    authRespKey: Bytes,
    ephemeralPubKey: SECP256K1.PublicKey
  ): AuthHeader {
    val plain = RLP.encodeList { writer ->
      writer.writeInt(VERSION)
      writer.writeValue(signature.bytes())
      writer.writeValue(enr) // TODO: Seq number if enrSeq from WHOAREYOU is equal to local, else nothing
    }
    val zeroNonce = Bytes.wrap(ByteArray(ZERO_NONCE_SIZE))
    val authResponse = AES128GCM.encrypt(authRespKey, zeroNonce, plain, Bytes.EMPTY)

    return AuthHeader(params.authTag, params.idNonce, ephemeralPubKey.bytes(), Bytes.wrap(authResponse))
  }

  companion object {
    private const val SESSION_KEY_EXPIRATION: Long = 5

    private const val ZERO_NONCE_SIZE: Int = 12
    private const val VERSION: Int = 5

    private val DISCOVERY_ID_NONCE: Bytes = Bytes.wrap("discovery-id-nonce".toByteArray())
  }
}
