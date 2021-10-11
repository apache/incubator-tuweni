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
package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord

class OrdinaryPacket(private val srcId: Bytes32, private val messageData: Bytes) {
  val nonce = Bytes.random(12)

  fun toBytes() = Bytes.concatenate(
    Bytes.random(16), Bytes.wrap("discv5".toByteArray()), Bytes.fromHexString("0x0001"),
    Bytes.of(0x00), nonce, Bytes.of(0, 32), srcId, messageData
  )
}

class WhoAreYouPacket(private val nonce: Bytes, private val enrSeq: Long, val idNonce: Bytes = Bytes.random(16)) {
  fun toBytes() = Bytes.concatenate(
    Bytes.random(16), Bytes.wrap("discv5".toByteArray()), Bytes.fromHexString("0x0001"),
    Bytes.of(0x01), nonce, Bytes.fromHexString("0x0018"), idNonce, Bytes.ofUnsignedLong(enrSeq)
  )
}

class HandshakeMessagePacket(private val srcId: Bytes32, private val ephemeralPublicKey: SECP256K1.PublicKey, private val idSignature: Bytes, private val enrRecord: EthereumNodeRecord, private val messageData: Bytes) {
  fun toBytes(): Bytes {
    val authdatasize = 34 + 64 + 33 + enrRecord.toRLP().size()

    return Bytes.concatenate(
      Bytes.random(16), Bytes.wrap("discv5".toByteArray()), Bytes.fromHexString("0x0001"),
      Bytes.of(0x02), Bytes.random(12),
      Bytes.of((authdatasize shr 8).toByte(), authdatasize.toByte()),
      srcId,
      Bytes.fromHexString("0x40"), // 64 size of the v4 signature
      Bytes.fromHexString("0x21"),
      idSignature,
      Bytes.wrap(ephemeralPublicKey.asEcPoint().getEncoded(true)),
      enrRecord.toRLP(), messageData
    )
  }
}
