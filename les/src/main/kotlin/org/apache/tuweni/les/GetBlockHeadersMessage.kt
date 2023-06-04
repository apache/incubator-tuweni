// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.les

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.units.bigints.UInt256

internal data class GetBlockHeadersMessage(val reqID: Long, val queries: List<BlockHeaderQuery>) {

  internal data class BlockHeaderQuery(
    val blockNumberOrBlockHash: Bytes32,
    val maxHeaders: UInt256,
    val skip: UInt256,
    val direction: Direction
  ) {

    internal enum class Direction {
      BACKWARDS, FORWARD
    }
  }

  fun toBytes(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeLong(reqID)
      for (query in queries) {
        writer.writeList { queryWriter ->
          queryWriter.writeValue(query.blockNumberOrBlockHash)
          queryWriter.writeUInt256(query.maxHeaders)
          queryWriter.writeUInt256(query.skip)
          queryWriter.writeInt(if (query.direction == BlockHeaderQuery.Direction.BACKWARDS) 1 else 0)
        }
      }
    }
  }

  companion object {

    fun read(bytes: Bytes): GetBlockHeadersMessage {
      return RLP.decodeList(bytes) { reader ->
        val reqId = reader.readLong()
        val queries = ArrayList<BlockHeaderQuery>()
        while (!reader.isComplete) {
          queries.add(
            reader.readList { queryReader ->
              BlockHeaderQuery(
                Bytes32.wrap(queryReader.readValue()),
                queryReader.readUInt256(),
                queryReader.readUInt256(),
                if (queryReader.readInt() == 1) {
                  BlockHeaderQuery.Direction.BACKWARDS
                } else {
                  BlockHeaderQuery.Direction.FORWARD
                }
              )
            }
          )
        }
        GetBlockHeadersMessage(reqId, queries)
      }
    }
  }
}
