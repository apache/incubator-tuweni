// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.v5.encrypt

import org.apache.tuweni.bytes.Bytes
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters

/**
 * Generates session keys on handshake, using HKDF key derivation function
 */
internal object SessionKeyGenerator {

  private const val DERIVED_KEY_SIZE: Int = 16
  private val INFO_PREFIX = Bytes.wrap("discovery v5 key agreement".toByteArray())

  /**
   * Executes session keys generation
   *
   * @param srcNodeId sender node identifier
   * @param destNodeId receiver node identifier
   * @param secret the input keying material or seed
   * @param idNonce nonce used as salt
   */
  fun generate(srcNodeId: Bytes, destNodeId: Bytes, secret: Bytes, idNonce: Bytes): SessionKey {
    val info = Bytes.concatenate(INFO_PREFIX, srcNodeId, destNodeId)

    val hkdf = HKDFBytesGenerator(SHA256Digest())
    val params = HKDFParameters(secret.toArrayUnsafe(), idNonce.toArrayUnsafe(), info.toArrayUnsafe())
    hkdf.init(params)
    val output = Bytes.wrap(ByteArray(DERIVED_KEY_SIZE * 3))
    hkdf.generateBytes(output.toArrayUnsafe(), 0, output.size())
    return SessionKey(
      output.slice(0, DERIVED_KEY_SIZE),
      output.slice(DERIVED_KEY_SIZE, DERIVED_KEY_SIZE),
      output.slice(DERIVED_KEY_SIZE * 2),
    )
  }
}
