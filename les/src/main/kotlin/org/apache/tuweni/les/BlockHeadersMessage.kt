// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
