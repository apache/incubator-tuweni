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
package org.apache.tuweni.devp2p.eth

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.units.bigints.UInt256

/**
 * ETH subprotocol message types.
 */
enum class MessageType(val code: Int) {
  Status(0x00),
  NewBlockHashes(0x01),
  Transactions(0x02),
  GetBlockHeaders(0x03),
  BlockHeaders(0x04),
  GetBlockBodies(0x05),
  BlockBodies(0x06),
  NewBlock(0x07),
  GetNodeData(0x0d),
  NodeData(0x0e),
  GetReceipts(0x0f),
  Receipts(0x10)
}

data class StatusMessage(
  val protocolVersion: Int,
  val networkID: UInt256,
  val totalDifficulty: UInt256,
  val bestHash: Hash,
  val genesisHash: Hash,
  val forkHash: Bytes?,
  val forkBlock: Long?
) {

  companion object {

    fun read(payload: Bytes): StatusMessage = RLP.decode(payload) {
      it.readList { reader ->
        val protocolVersion = reader.readInt()
        val networkID = UInt256.fromBytes(reader.readValue())
        val totalDifficulty = UInt256.fromBytes(reader.readValue())
        val bestHash = Hash.fromBytes(reader.readValue())
        val genesisHash = Hash.fromBytes(reader.readValue())
        val forkInfo = reader.readList { fork ->
          Pair(fork.readValue(), fork.readValue())
        }

        StatusMessage(
          protocolVersion, networkID, totalDifficulty, bestHash,
          genesisHash, forkInfo.first, forkInfo.second.toLong()
        )
      }
    }
  }

  fun toBytes(): Bytes = RLP.encodeList {
    it.writeInt(protocolVersion)
    it.writeUInt256(networkID)
    it.writeUInt256(totalDifficulty)
    it.writeValue(bestHash)
    it.writeValue(genesisHash)
    it.writeList { forkWriter ->
      forkWriter.writeValue(Bytes.fromHexString("0xe029e991"))
      forkWriter.writeValue(Bytes.ofUnsignedLong(forkBlock!!).trimLeadingZeros())
    }
  }
}

data class NewBlockHashes(val hashes: List<Pair<Hash, Long>>) {

  companion object {

    fun read(payload: Bytes): NewBlockHashes = RLP.decodeList(payload) {
      val hashes = ArrayList<Pair<Hash, Long>>()
      while (!it.isComplete) {
        hashes.add(it.readList { pairReader ->
          Pair(Hash.fromBytes(pairReader.readValue()), pairReader.readLong())
        })
      }
      NewBlockHashes(hashes)
    }
  }

  fun toBytes(): Bytes = RLP.encodeList { writer ->
    hashes.forEach { pair ->
      writer.writeList {
        it.writeValue(pair.first)
        it.writeLong(pair.second)
      }
    }
  }
}

data class GetBlockHeaders(val hash: Hash, val maxHeaders: Long, val skip: Long, val reverse: Boolean) {
  companion object {

    fun read(payload: Bytes): GetBlockHeaders = RLP.decodeList(payload) {
      val hash = Hash.fromBytes(it.readValue())
      val maxHeaders = it.readLong()
      val skip = it.readLong()
      val reverse = it.readInt() == 1
      GetBlockHeaders(hash, maxHeaders, skip, reverse)
    }
  }

  fun toBytes(): Bytes = RLP.encodeList { writer ->
    writer.writeValue(hash)
    writer.writeLong(maxHeaders)
    writer.writeLong(skip)
    writer.writeInt(if (reverse) 1 else 0)
  }
}

data class BlockHeaders(val headers: List<BlockHeader>) {
  companion object {

    fun read(payload: Bytes): BlockHeaders = RLP.decodeList(payload) {
      val headers = ArrayList<BlockHeader>()
      while (!it.isComplete) {
        headers.add(it.readList { rlp ->
          BlockHeader.readFrom(rlp)
        })
      }
      BlockHeaders(headers)
    }
  }

  fun toBytes(): Bytes = RLP.encodeList { writer ->
    headers.forEach {
      writer.writeRLP(it.toBytes())
    }
  }
}

data class GetBlockBodies(val hashes: List<Hash>) {
  companion object {

    fun read(payload: Bytes): GetBlockBodies = RLP.decodeList(payload) {
      val hashes = ArrayList<Hash>()
      while (!it.isComplete) {
        hashes.add(Hash.fromBytes(it.readValue()))
      }
      GetBlockBodies(hashes)
    }
  }

  fun toBytes(): Bytes = RLP.encodeList { writer ->
    hashes.forEach {
      writer.writeValue(it)
    }
  }
}

data class BlockBodies(val bodies: List<BlockBody>) {
  companion object {

    fun read(payload: Bytes): BlockBodies = RLP.decodeList(payload) {
      val bodies = ArrayList<BlockBody>()
      while (!it.isComplete) {
        bodies.add(it.readList { rlp ->
          BlockBody.readFrom(rlp)
        })
      }
      BlockBodies(bodies)
    }
  }

  fun toBytes(): Bytes = RLP.encodeList { writer ->
    bodies.forEach {
      writer.writeRLP(it.toBytes())
    }
  }
}

data class NewBlock(val block: Block, val totalDifficulty: UInt256) {
  companion object {

    fun read(payload: Bytes): NewBlock = RLP.decodeList(payload) {
      val block = it.readList { reader -> Block.readFrom(reader) }
      val difficulty = it.readUInt256()
      NewBlock(block, difficulty)
    }
  }

  fun toBytes(): Bytes = RLP.encodeList { writer ->
    writer.writeRLP(block.toBytes())
    writer.writeUInt256(totalDifficulty)
  }
}

data class GetNodeData(val hashes: List<Hash>) {
  companion object {

    fun read(payload: Bytes): GetNodeData = RLP.decodeList(payload) {
      val hashes = ArrayList<Hash>()
      while (!it.isComplete) {
        hashes.add(Hash.fromBytes(it.readValue()))
      }
      GetNodeData(hashes)
    }
  }

  fun toBytes(): Bytes = RLP.encodeList { writer ->
    hashes.forEach {
      writer.writeValue(it)
    }
  }
}

data class NodeData(val elements: List<Any>) {
  fun toBytes(): Bytes = RLP.encodeList { _ ->
  }
}

data class GetReceipts(val hashes: List<Hash>) {
  companion object {

    fun read(payload: Bytes): GetReceipts = RLP.decodeList(payload) {
      val hashes = ArrayList<Hash>()
      while (!it.isComplete) {
        hashes.add(Hash.fromBytes(it.readValue()))
      }
      GetReceipts(hashes)
    }
  }

  fun toBytes(): Bytes = RLP.encodeList { writer ->
    hashes.forEach {
      writer.writeValue(it)
    }
  }
}

data class Receipts(val transactionReceipts: List<List<TransactionReceipt>>) {
  companion object {

    fun read(payload: Bytes): Receipts = RLP.decodeList(payload) {
      val transactionReceipts = ArrayList<List<TransactionReceipt>>()
      while (!it.isComplete) {
        val list = ArrayList<TransactionReceipt>()
        transactionReceipts.add(list)
        it.readList { sublist ->
          while (!sublist.isComplete) {
            list.add(TransactionReceipt.readFrom(sublist))
          }
        }
      }
      Receipts(transactionReceipts)
    }
  }

  fun toBytes(): Bytes = RLP.encodeList { writer ->
    transactionReceipts.forEach {
      writer.writeList { listWriter ->
        it.forEach {
          listWriter.writeRLP(it.toBytes())
        }
      }
    }
  }
}
