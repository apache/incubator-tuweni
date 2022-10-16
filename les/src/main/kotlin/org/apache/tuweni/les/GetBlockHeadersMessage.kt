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
