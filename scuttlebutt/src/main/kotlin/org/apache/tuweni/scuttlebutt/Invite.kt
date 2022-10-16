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
package org.apache.tuweni.scuttlebutt

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.sodium.Signature
import java.util.Arrays

/**
 * An invite code as defined by the Secure Scuttlebutt protocol guide.
 *
 *
 * See https://ssbc.github.io/scuttlebutt-protocol-guide/ for a detailed description of invites.
 *
 * @param host the host to connect to
 * @param port the port of the host to connect to
 * @param identity the public key of the host to connect to
 * @param seedKey an Ed25519 secret key used to identify the invite
 */
class Invite(val host: String, val port: Int, val identity: Identity, val seedKey: Signature.Seed) {

  companion object {
    /**
     * Takes an invite string in its canonical form and parses it into an Invite object, throwing an error if it is not of
     * the expected format.
     *
     * @param inviteCode the invite code in its canonical form
     * @return the invite code
     */
    @JvmStatic
    fun fromCanonicalForm(inviteCode: String): Invite {
      val exceptionMessage = "Invite code should be of format host:port:publicKey.curveName~secretKey"
      val parts = Arrays.asList(*inviteCode.split(":".toRegex()).toTypedArray())
      if (parts.size != 3) {
        throw MalformedInviteCodeException(exceptionMessage)
      }
      val host = parts[0]
      val portString = parts[1]
      val port = toPort(portString)
      val keyAndSecret = Arrays.asList(*parts[2].split("~".toRegex()).toTypedArray())
      if (keyAndSecret.size != 2) {
        throw MalformedInviteCodeException(exceptionMessage)
      }
      val fullKey = keyAndSecret[0]
      val splitKey = Arrays.asList(*fullKey.split("\\.".toRegex()).toTypedArray())
      if (splitKey.size != 2) {
        throw MalformedInviteCodeException(exceptionMessage)
      }
      val keyPart = splitKey[0]
      val secretKeyPart = keyAndSecret[1]
      val secretKey = toSecretKey(secretKeyPart)
      val identity = toPublicKey(keyPart)
      return Invite(host, port, identity, secretKey)
    }

    private fun toSecretKey(secretKeyPart: String): Signature.Seed {
      val secret = Bytes.fromBase64String(secretKeyPart)
      return Signature.Seed.fromBytes(secret)
    }

    private fun toPublicKey(keyPart: String): Ed25519PublicKeyIdentity {
      // Remove the @ from the front of the key
      val keyPartSuffix = keyPart.substring(1)
      val publicKeyBytes = Bytes.fromBase64String(keyPartSuffix)
      val publicKey = Signature.PublicKey.fromBytes(publicKeyBytes)
      return Ed25519PublicKeyIdentity(publicKey)
    }

    @Throws(MalformedInviteCodeException::class)
    private fun toPort(portString: String): Int {
      return try {
        portString.toInt()
      } catch (ex: NumberFormatException) {
        throw MalformedInviteCodeException("Expected a string for the port. Value parsed: $portString")
      }
    }
  }

  init {
    require(!(port <= 0 || port > 65535)) { "Invalid port" }
  }

  /**
   * Provides the invite as a string that is understood by other Secure Scuttlebutt clients.
   *
   * @return the canonical form of an invite.
   */
  fun toCanonicalForm(): String {
    return (
      host +
        ":" +
        port +
        ":" +
        "@" +
        identity.publicKeyAsBase64String() +
        "." +
        identity.curveName() +
        "~" +
        seedKey.bytes().toBase64String()
      )
  }

  override fun toString(): String {
    return toCanonicalForm()
  }
}
