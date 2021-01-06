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
package org.apache.tuweni.wallet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.crypto.sodium.AES256GCM
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

/**
 * Wallet containing a private key that is secured with symmetric encryption.
 *
 * This has not been audited for security concerns and should not be used in production.
 *
 * This wallet encrypts the key pair at rest, and encrypts the key in memory.
 *
 * Nonce is based on the password, should be instead stored in the wallet and unique.
 *
 * The wallet loads from a file.
 */
class Wallet(file: Path, password: String) {
  private val keyPair: SECP256K1.KeyPair

  init {
    val contents = Files.readString(file).trim()
    val encrypted = Bytes.fromHexString(contents)
    val hash = Hash.sha2_256(password.toByteArray(StandardCharsets.UTF_8))
    val nonceBytes = Hash.sha2_256(Bytes.wrap(Bytes.wrap(hash), Bytes.fromHexString("0A00E0"))).slice(0, 12)
    val key = AES256GCM.Key.fromBytes(hash)
    val nonce = AES256GCM.Nonce.fromBytes(nonceBytes)
    val decrypted = AES256GCM.decrypt(encrypted, key, nonce)
    keyPair =
      SECP256K1.KeyPair.fromSecretKey(
        SECP256K1.SecretKey.fromBytes(Bytes32.secure(decrypted!!.toArrayUnsafe()))
      )
  }

  companion object {
    /**
     * Creates a new wallet with that file, generating a private key and encrypting it with the password passed in.
     * @param file the file to create. If a file is present, it is rewritten.
     * @param password the password to encrypt the wallet with
     * @return the wallet created
     */
    fun create(file: Path, password: String): Wallet {
      val hash = Hash.sha2_256(password.toByteArray(StandardCharsets.UTF_8))
      val nonceBytes = Hash.sha2_256(Bytes.wrap(Bytes.wrap(hash), Bytes.fromHexString("0A00E0"))).slice(0, 12)
      val key = AES256GCM.Key.fromBytes(hash)
      val nonce = AES256GCM.Nonce.fromBytes(nonceBytes)
      val encrypted = AES256GCM.encrypt(Bytes32.random(), Bytes.EMPTY, key, nonce)
      file.parent.toFile().mkdirs()
      Files.write(
        file,
        encrypted.toHexString().encodeToByteArray(),
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
      )
      return open(file, password)
    }

    /**
     * Opens a new wallet with the file. If the file is missing, it throws an IllegalArgumentException.
     * @param file the file with the encrypted secret.
     * @param password the password
     * @return the wallet from the file
     */
    fun open(file: Path, password: String): Wallet {
      return Wallet(file, password)
    }
  }

  /**
   * Creates and signs a transaction
   */
  fun sign(
    nonce: UInt256,
    gasPrice: Wei,
    gasLimit: Gas,
    to: Address?,
    value: Wei,
    payload: Bytes,
    chainId: Int?
  ): Transaction {
    return Transaction(nonce, gasPrice, gasLimit, to, value, payload, keyPair, chainId)
  }

  fun verify(tx: Transaction): Boolean {
    val pubKey = tx.extractPublicKey()
    return keyPair.publicKey() == pubKey
  }

  fun address(): Address {
    return Address.fromPublicKey(keyPair.publicKey())
  }
}
