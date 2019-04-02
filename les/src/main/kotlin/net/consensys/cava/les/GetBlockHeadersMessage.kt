/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.les

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32
import net.consensys.cava.rlp.RLP
import net.consensys.cava.units.bigints.UInt256

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
                if (queryReader.readInt() == 1)
                  BlockHeaderQuery.Direction.BACKWARDS
                else
                  BlockHeaderQuery.Direction.FORWARD
              )
            })
        }
        GetBlockHeadersMessage(reqId, queries)
      }
    }
  }
}
