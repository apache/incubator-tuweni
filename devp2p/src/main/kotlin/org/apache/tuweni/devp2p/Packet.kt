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
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlp.RLPException
import org.apache.tuweni.rlp.RLPWriter
import java.nio.ByteBuffer

internal class DecodingException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal data class SigHash(val signature: SECP256K1.Signature, val hash: Bytes32)

private fun msecToSec(time: Long) = (time + 999) / 1000
private fun secToMsec(time: Long) = time * 1000

internal sealed class Packet(
  val nodeId: SECP256K1.PublicKey,
  private val signature: SECP256K1.Signature,
  val hash: Bytes32,
  val expiration: Long
) {

  companion object {
    const val MIN_SIZE = 104
    const val MAX_SIZE = 1280

    private const val HASH_INDEX = 0
    private const val SIGNATURE_INDEX = 32
    private const val PACKET_TYPE_INDEX = 97
    private const val PACKET_DATA_INDEX = 98

    fun decodeFrom(datagram: ByteBuffer) =
      decodeFrom(Bytes.wrapByteBuffer(datagram))

    fun decodeFrom(datagram: Bytes): Packet {
      val typeByte = datagram.get(PACKET_TYPE_INDEX)
      val packetType = PacketType.forType(typeByte) ?: throw DecodingException("Unrecognized packet type: $typeByte")

      val signature = SECP256K1.Signature.fromBytes(
        datagram.slice(SIGNATURE_INDEX, PACKET_TYPE_INDEX - SIGNATURE_INDEX)
      )

      val publicKey = SECP256K1.PublicKey.recoverFromSignature(
        datagram.slice(PACKET_TYPE_INDEX, datagram.size() - PACKET_TYPE_INDEX),
        signature
      ) ?: throw DecodingException("Invalid packet signature")

      val hash = Bytes32.wrap(
        datagram.slice(
          HASH_INDEX,
          SIGNATURE_INDEX
        )
      )
      if (Hash.keccak256(datagram.slice(SIGNATURE_INDEX)) != hash) {
        throw DecodingException("Invalid packet hash")
      }

      return packetType.decode(datagram.slice(PACKET_DATA_INDEX), hash, publicKey, signature)
    }

    @JvmStatic
    protected fun expirationFor(now: Long) = now + PACKET_EXPIRATION_PERIOD_MS

    @JvmStatic
    protected fun createSignature(
      packetType: PacketType,
      keyPair: SECP256K1.KeyPair,
      encoder: (RLPWriter) -> Unit
    ): SigHash {
      val typeByte = Bytes.of(packetType.typeId)
      val dataBytes = RLP.encodeList { writer -> encoder(writer) }
      val payloadBytes = Bytes.wrap(typeByte, dataBytes)
      val signature = SECP256K1.sign(payloadBytes, keyPair)
      val hash = Hash.keccak256(Bytes.wrap(signature.bytes(), payloadBytes))
      return SigHash(signature, hash)
    }
  }

  fun isExpired(now: Long): Boolean = expiration <= now

  abstract fun encode(): Bytes

  protected fun encodeTo(packetType: PacketType, contentWriter: (RLPWriter) -> Unit): Bytes {
    return Bytes.wrap(hash, signature.bytes(), Bytes.of(packetType.typeId), RLP.encodeList(contentWriter))
  }
}

internal class PingPacket private constructor(
  nodeId: SECP256K1.PublicKey,
  signature: SECP256K1.Signature,
  hash: Bytes32,
  val from: Endpoint,
  val to: Endpoint,
  expiration: Long,
  val enrSeq: Long?
) : Packet(nodeId, signature, hash, expiration) {

  companion object {
    private const val VERSION = 4

    fun create(keyPair: SECP256K1.KeyPair, now: Long, from: Endpoint, to: Endpoint, seq: Long?): PingPacket {
      val expiration = expirationFor(now)
      val sigHash = createSignature(
        PacketType.PING,
        keyPair
      ) { writer ->
        encodeTo(writer, from, to, expiration, seq)
      }
      return PingPacket(
        keyPair.publicKey(),
        sigHash.signature,
        sigHash.hash,
        from,
        to,
        expiration,
        seq
      )
    }

    fun decode(
      payload: Bytes,
      hash: Bytes32,
      publicKey: SECP256K1.PublicKey,
      signature: SECP256K1.Signature
    ): PingPacket {
      try {
        return RLP.decodeList(payload) { reader ->
          val version = reader.readInt()
          val from = reader.readList { r -> Endpoint.readFrom(r) }
          val to = reader.readList { r -> Endpoint.readFrom(r) }
          val expiration = reader.readLong() // seconds
          val seq: Long?
          if (!reader.isComplete) {
            seq = reader.readLong()
          } else {
            seq = null
          }

          if (version < VERSION) {
            throw DecodingException("Unexpected version $VERSION in ping")
          }
          PingPacket(publicKey, signature, hash, from, to, secToMsec(expiration), seq)
        }
      } catch (e: RLPException) {
        throw DecodingException("Invalid ping packet", e)
      }
    }

    private fun encodeTo(writer: RLPWriter, from: Endpoint, to: Endpoint, expiration: Long, seq: Long?) {
      writer.writeInt(VERSION)
      writer.writeList { w -> from.writeTo(w) }
      writer.writeList { w -> to.writeTo(w) }
      writer.writeLong(msecToSec(expiration)) // write in seconds
      seq?.let {
        writer.writeLong(it)
      }
    }
  }

  override fun encode() = encodeTo(PacketType.PING) { writer ->
    encodeTo(writer, from, to, expiration, enrSeq)
  }
}

internal class PongPacket private constructor(
  nodeId: SECP256K1.PublicKey,
  signature: SECP256K1.Signature,
  hash: Bytes32,
  val to: Endpoint,
  val pingHash: Bytes32,
  expiration: Long,
  val enrSeq: Long?
) : Packet(nodeId, signature, hash, expiration) {

  companion object {
    fun create(keyPair: SECP256K1.KeyPair, now: Long, to: Endpoint, pingHash: Bytes32, enrSeq: Long?): PongPacket {
      val expiration = expirationFor(now)
      val sigHash = createSignature(
        PacketType.PONG,
        keyPair
      ) { writer ->
        encodeTo(writer, to, pingHash, expiration, enrSeq)
      }
      return PongPacket(
        keyPair.publicKey(),
        sigHash.signature,
        sigHash.hash,
        to,
        pingHash,
        expiration,
        enrSeq
      )
    }

    fun decode(
      payload: Bytes,
      hash: Bytes32,
      publicKey: SECP256K1.PublicKey,
      signature: SECP256K1.Signature
    ): PongPacket {
      try {
        return RLP.decodeList(payload) { reader ->
          val to = reader.readList { r -> Endpoint.readFrom(r) }
          val pingHash = Bytes32.wrap(reader.readValue())
          val expiration = reader.readLong() // seconds
          val seq: Long?
          if (!reader.isComplete) {
            seq = reader.readLong()
          } else {
            seq = null
          }
          PongPacket(publicKey, signature, hash, to, pingHash, secToMsec(expiration), seq)
        }
      } catch (e: RLPException) {
        throw DecodingException("Invalid pong packet", e)
      }
    }

    private fun encodeTo(writer: RLPWriter, to: Endpoint, pingHash: Bytes32, expiration: Long, enrSeq: Long?) {
      writer.writeList { w -> to.writeTo(w) }
      writer.writeValue(pingHash)
      writer.writeLong(msecToSec(expiration))
      enrSeq?.let { writer.writeLong(it) }
    }
  }

  override fun encode() = encodeTo(PacketType.PONG) { writer ->
    encodeTo(writer, to, pingHash, expiration, enrSeq)
  }
}

internal class FindNodePacket private constructor(
  nodeId: SECP256K1.PublicKey,
  signature: SECP256K1.Signature,
  hash: Bytes32,
  val target: SECP256K1.PublicKey,
  expiration: Long
) : Packet(nodeId, signature, hash, expiration) {

  companion object {
    fun create(keyPair: SECP256K1.KeyPair, now: Long, target: SECP256K1.PublicKey): FindNodePacket {
      val expiration = expirationFor(now)
      val sigHash = createSignature(
        PacketType.FIND_NODE,
        keyPair
      ) { writer ->
        encodeTo(writer, target, expiration)
      }
      return FindNodePacket(
        keyPair.publicKey(),
        sigHash.signature,
        sigHash.hash,
        target,
        expiration
      )
    }

    fun decode(
      payload: Bytes,
      hash: Bytes32,
      publicKey: SECP256K1.PublicKey,
      signature: SECP256K1.Signature
    ): FindNodePacket {
      try {
        return RLP.decodeList(payload) { reader ->
          val target = SECP256K1.PublicKey.fromBytes(reader.readValue())
          val expiration = reader.readLong()
          FindNodePacket(publicKey, signature, hash, target, secToMsec(expiration))
        }
      } catch (e: RLPException) {
        throw DecodingException("Invalid find nodes packet", e)
      }
    }

    private fun encodeTo(writer: RLPWriter, target: SECP256K1.PublicKey, expiration: Long) {
      writer.writeValue(target.bytes())
      writer.writeLong(msecToSec(expiration))
    }
  }

  override fun encode() = encodeTo(PacketType.FIND_NODE) { writer ->
    encodeTo(writer, target, expiration)
  }
}

internal class NeighborsPacket private constructor(
  nodeId: SECP256K1.PublicKey,
  signature: SECP256K1.Signature,
  hash: Bytes32,
  val nodes: List<Node>,
  expiration: Long
) : Packet(nodeId, signature, hash, expiration) {

  companion object {
    // an over-estimate of the minimum size, based on a full 64-bit expiration time and a full list length prefix
    internal const val RLP_MIN_SIZE = 109

    fun create(keyPair: SECP256K1.KeyPair, now: Long, nodes: List<Node>): NeighborsPacket {
      val expiration = expirationFor(now)
      val sigHash = createSignature(
        PacketType.NEIGHBORS,
        keyPair
      ) { writer ->
        encodeTo(writer, nodes, expiration)
      }
      return NeighborsPacket(
        keyPair.publicKey(),
        sigHash.signature,
        sigHash.hash,
        nodes,
        expiration
      )
    }

    fun createRequired(keyPair: SECP256K1.KeyPair, now: Long, nodes: List<Node>): List<NeighborsPacket> {
      val result = mutableListOf<NeighborsPacket>()
      var nodeSubset = mutableListOf<Node>()
      var size = RLP_MIN_SIZE
      for (node in nodes) {
        val nodeSize = node.rlpSize()
        size += nodeSize
        if (size > MAX_SIZE) {
          result.add(create(keyPair, now, nodeSubset))
          nodeSubset = mutableListOf()
          size = RLP_MIN_SIZE + nodeSize
        }
        nodeSubset.add(node)
      }
      result.add(create(keyPair, now, nodeSubset))
      return result
    }

    fun decode(
      payload: Bytes,
      hash: Bytes32,
      publicKey: SECP256K1.PublicKey,
      signature: SECP256K1.Signature
    ): NeighborsPacket {
      try {
        return RLP.decodeList(payload) { reader ->
          val nodes = mutableListOf<Node>()
          reader.readList { r ->
            while (!r.isComplete) {
              val node = r.readList { nr -> Node.readFrom(nr) }
              nodes.add(node)
            }
          }
          val expiration = reader.readLong()
          NeighborsPacket(publicKey, signature, hash, nodes, secToMsec(expiration))
        }
      } catch (e: RLPException) {
        throw DecodingException("Invalid nodes packet", e)
      }
    }

    private fun encodeTo(writer: RLPWriter, nodes: List<Node>, expiration: Long) {
      writer.writeList { w -> nodes.forEach { node -> w.writeList { nw -> node.writeTo(nw) } } }
      writer.writeLong(msecToSec(expiration))
    }
  }

  override fun encode() = encodeTo(PacketType.NEIGHBORS) { writer ->
    encodeTo(writer, nodes, expiration)
  }
}

internal class ENRRequestPacket private constructor(
  nodeId: SECP256K1.PublicKey,
  signature: SECP256K1.Signature,
  hash: Bytes32,
  expiration: Long
) : Packet(nodeId, signature, hash, expiration) {

  companion object {
    fun decode(
      payload: Bytes,
      hash: Bytes32,
      publicKey: SECP256K1.PublicKey,
      signature: SECP256K1.Signature
    ): ENRRequestPacket {
      try {
        return RLP.decodeList(payload) { reader ->
          val expiration = reader.readLong()
          ENRRequestPacket(publicKey, signature, hash, secToMsec(expiration))
        }
      } catch (e: RLPException) {
        throw DecodingException("Invalid enr request packet", e)
      }
    }

    private fun encodeTo(writer: RLPWriter, expiration: Long) {
      writer.writeLong(msecToSec(expiration))
    }

    fun create(keyPair: SECP256K1.KeyPair, now: Long): Packet {
      val expiration = expirationFor(now)
      val sigHash = createSignature(
        PacketType.ENRRESPONSE,
        keyPair
      ) { writer ->
        ENRRequestPacket.encodeTo(writer, expiration)
      }
      return ENRRequestPacket(
        keyPair.publicKey(),
        sigHash.signature,
        sigHash.hash,
        expiration
      )
    }
  }

  override fun encode() = encodeTo(PacketType.ENRREQUEST) { writer ->
    encodeTo(writer, expiration)
  }
}

internal class ENRResponsePacket private constructor(
  nodeId: SECP256K1.PublicKey,
  signature: SECP256K1.Signature,
  hash: Bytes32,
  expiration: Long,
  val requestHash: Bytes,
  val enr: Bytes
) : Packet(nodeId, signature, hash, expiration) {

  companion object {

    fun create(keyPair: SECP256K1.KeyPair, now: Long, requestHash: Bytes, enr: Bytes): ENRResponsePacket {
      val expiration = expirationFor(now)
      val sigHash = createSignature(
        PacketType.ENRRESPONSE,
        keyPair
      ) { writer ->
        encodeTo(writer, requestHash, enr, expiration)
      }
      return ENRResponsePacket(
        keyPair.publicKey(),
        sigHash.signature,
        sigHash.hash,
        expiration,
        requestHash,
        enr
      )
    }

    fun decode(
      payload: Bytes,
      hash: Bytes32,
      publicKey: SECP256K1.PublicKey,
      signature: SECP256K1.Signature
    ): ENRResponsePacket {
      try {
        return RLP.decodeList(payload) { reader ->
          // request-hash, ENR
          val requestHash = reader.readValue()
          val enr = reader.readValue()
          val expiration = reader.readLong()
          ENRResponsePacket(publicKey, signature, hash, secToMsec(expiration), requestHash, enr)
        }
      } catch (e: RLPException) {
        throw DecodingException("Invalid enr response packet", e)
      }
    }

    private fun encodeTo(writer: RLPWriter, requestHash: Bytes, enr: Bytes, expiration: Long) {
      writer.writeValue(requestHash)
      writer.writeValue(enr)
      writer.writeLong(msecToSec(expiration))
    }
  }

  override fun encode() = encodeTo(PacketType.ENRRESPONSE) { writer ->
    ENRResponsePacket.encodeTo(writer, requestHash, enr, expiration)
  }
}
