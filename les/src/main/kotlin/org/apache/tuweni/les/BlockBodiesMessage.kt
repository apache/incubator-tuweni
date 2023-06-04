// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.les

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.rlp.RLP

internal data class BlockBodiesMessage(
  val reqID: Long,
  val bufferValue: Long,
  val blockBodies: List<BlockBody>
) {

  fun toBytes(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeLong(reqID)
      writer.writeLong(bufferValue)
      writer.writeList(blockBodies) { eltWriter, blockBody -> blockBody.writeTo(eltWriter) }
    }
  }

  companion object {

    fun read(bytes: Bytes): BlockBodiesMessage {
      return RLP.decodeList(
        bytes
      ) { reader ->
        BlockBodiesMessage(
          reader.readLong(),
          reader.readLong(),
          reader.readListContents { BlockBody.readFrom(it) }
        )
      }
    }
  }
}
