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
package org.apache.tuweni.devp2p

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.MutableBytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlp.RLPWriter
import org.apache.tuweni.units.bigints.UInt256
import java.lang.IllegalArgumentException
import java.lang.RuntimeException
import java.net.InetAddress
import java.time.Instant

/**
 * Ethereum Node Record (ENR) as described in [EIP-778](https://eips.ethereum.org/EIPS/eip-778).
 *
 * @param signature the record signature
 * @param seq the sequence of the record, its revision number
 * @param data the arbitrary data of the record
 * @param listData the arbitrary data of the record as list
 */
class EthereumNodeRecord(
  val signature: Bytes,
  val seq: Long,
  val data: Map<String, Bytes>,
  val listData: Map<String, List<Bytes>> = emptyMap()
) {

  companion object {

    /**
     * Creates an ENR from its serialized form as a RLP list
     * @param rlp the serialized form of the ENR
     * @return the ENR
     * @throws IllegalArgumentException if the rlp bytes length is longer than 300 bytes
     */
    @JvmStatic
    fun fromRLP(rlp: Bytes): EthereumNodeRecord {
      if (rlp.size() > 300) {
        throw IllegalArgumentException("Record too long")
      }
      return RLP.decodeList(rlp) {
          val sig = it.readValue()

          val seq = it.readLong()

          val data = mutableMapOf<String, Bytes>()
          val listData = mutableMapOf<String, List<Bytes>>()
          while (!it.isComplete) {
            val key = it.readString()
            if (it.nextIsList()) {
              listData[key] = it.readListContents { listreader ->
                if (listreader.nextIsList()) {
                  // TODO complex structures not supported
                  listreader.skipNext()
                  null
                } else {
                  listreader.readValue()
                }
              }.filterNotNull()
            } else {
              val value = it.readValue()
              data[key] = value
            }
          }

          EthereumNodeRecord(sig, seq, data, listData)
      }
    }

    private fun encode(
      signatureKeyPair: SECP256K1.KeyPair? = null,
      seq: Long = Instant.now().toEpochMilli(),
      ip: InetAddress? = null,
      tcp: Int? = null,
      udp: Int? = null,
      data: Map<String, Bytes>? = null,
      listData: Map<String, List<Bytes>>? = null,
      writer: RLPWriter
    ) {
      writer.writeLong(seq)
      val mutableData = data?.toMutableMap() ?: mutableMapOf()
      mutableData["id"] = Bytes.wrap("v4".toByteArray())
      signatureKeyPair?.let {
        mutableData["secp256k1"] = Bytes.wrap(it.publicKey().asEcPoint().getEncoded(true))
      }
      ip?.let {
        mutableData["ip"] = Bytes.wrap(it.address)
      }
      tcp?.let {
        mutableData["tcp"] = Bytes.ofUnsignedShort(it)
      }
      udp?.let {
        mutableData["udp"] = Bytes.ofUnsignedShort(it)
      }
      val keys = mutableListOf<String>()
      keys.addAll(mutableData.keys)
      listData?.let { keys.addAll(it.keys) }
      keys.sorted().forEach { key ->
          mutableData[key]?.let { value ->
            writer.writeString(key)
            writer.writeValue(value)
          }
          listData?.get(key)?.let { value ->
          writer.writeString(key)
          writer.writeList(value) { writer, v -> writer.writeValue(v) }
          }
      }
    }

    /**
     * Creates the serialized form of a ENR
     * @param signatureKeyPair the key pair to use to sign the ENR
     * @param seq the sequence number for the ENR. It should be higher than the previous time the ENR was generated. It defaults to the current time since epoch in milliseconds.
     * @param data the key pairs to encode in the ENR
     * @param listData the key pairs of list values to encode in the ENR
     * @param ip the IP address of the host
     * @param tcp an optional parameter to a TCP port used for the wire protocol
     * @param udp an optional parameter to a UDP port used for discovery
     * @return the serialized form of the ENR as a RLP-encoded list
     */
    @JvmOverloads
    @JvmStatic
    fun toRLP(
      signatureKeyPair: SECP256K1.KeyPair,
      seq: Long = Instant.now().toEpochMilli(),
      data: Map<String, Bytes>? = null,
      listData: Map<String, List<Bytes>>? = null,
      ip: InetAddress,
      tcp: Int? = null,
      udp: Int? = null
    ): Bytes {
      val encoded = RLP.encode { writer ->
        encode(signatureKeyPair, seq, ip, tcp, udp, data, listData, writer)
      }
      val signature = SECP256K1.sign(Hash.keccak256(encoded), signatureKeyPair)
      val sigBytes = MutableBytes.create(64)
      UInt256.valueOf(signature.r()).toBytes().copyTo(sigBytes, 0)
      UInt256.valueOf(signature.s()).toBytes().copyTo(sigBytes, 32)

      val completeEncoding = RLP.encodeList { writer ->
        writer.writeValue(sigBytes)
        encode(signatureKeyPair, seq, ip, tcp, udp, data, listData, writer)
      }
      return completeEncoding
    }
  }

  /**
   * Validates an ENR to check that it conforms to a valid ENR scheme.
   *
   * Only the v4 scheme is supported at this time.
   */
  fun validate() {
    if (Bytes.wrap("v4".toByteArray()) != data["id"]) {
      throw InvalidNodeRecordException("id attribute is not set to v4")
    }

    val encoded = RLP.encodeList {
      encode(data = data, seq = seq, writer = it)
    }

    val sig = SECP256K1.Signature.create(1, signature.slice(0, 32).toUnsignedBigInteger(),
      signature.slice(32).toUnsignedBigInteger())

    val pubKey = publicKey()

    val recovered = SECP256K1.PublicKey.recoverFromSignature(encoded, sig)

    if (pubKey != recovered) {
      throw InvalidNodeRecordException("Public key does not match signature")
    }
  }

  /**
   * The ENR public key entry
   * @return the ENR public key
   */
  fun publicKey(): SECP256K1.PublicKey {
    val keyBytes = data["secp256k1"] ?: throw InvalidNodeRecordException("Missing secp256k1 entry")
    val ecPoint = SECP256K1.Parameters.CURVE.getCurve().decodePoint(keyBytes.toArrayUnsafe())
    return SECP256K1.PublicKey.fromBytes(Bytes.wrap(ecPoint.getEncoded(false)).slice(1))
  }

  /**
   * The ip associated with the ENR
   * @return The IP adress of the ENR
   */
  fun ip(): InetAddress {
    return InetAddress.getByAddress(data["ip"]!!.toArrayUnsafe())
  }

  /**
   * The TCP port of the ENR
   * @return the TCP port associated with this ENR
   */
  fun tcp(): Int {
    return data["tcp"]!!.toInt()
  }

  /**
   * The UDP port of the ENR
   * @return the UDP port associated with this ENR
   */
  fun udp(): Int {
    return data["udp"]!!.toInt()
  }

  /**
   * @return the ENR as a URI
   */
  override fun toString(): String {
    return "enr:${ip()}:${tcp()}?udp=${udp()}"
  }
}

internal class InvalidNodeRecordException(message: String?) : RuntimeException(message)
