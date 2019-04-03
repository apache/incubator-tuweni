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
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.rlp.RLP

internal data class BlockHeadersMessage(
  val reqID: Long,
  val bufferValue: Long,
  val blockHeaders: List<BlockHeader>
) {

  fun toBytes(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeLong(reqID)
      writer.writeLong(bufferValue)
      writer.writeList { headersWriter ->
        for (bh in blockHeaders) {
          headersWriter.writeRLP(bh.toBytes())
        }
      }
    }
  }

  companion object {

    fun read(bytes: Bytes): BlockHeadersMessage {
      return RLP.decodeList(bytes) { reader ->
        val reqID = reader.readLong()
        val bufferValue = reader.readLong()
        val headers = reader.readListContents { headersReader ->
          headersReader.readList<BlockHeader> {
            BlockHeader.readFrom(it)
          }
        }
        BlockHeadersMessage(reqID, bufferValue, headers)
      }
    }
  }
}
