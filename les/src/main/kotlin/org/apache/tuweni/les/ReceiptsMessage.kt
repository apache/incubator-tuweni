// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.les

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.rlp.RLP

internal data class ReceiptsMessage(
  val reqID: Long,
  val bufferValue: Long,
  val receipts: List<List<TransactionReceipt>>
) {

  fun toBytes(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeLong(reqID)
      writer.writeLong(bufferValue)
      writer.writeList(receipts) {
          eltWriter, listOfReceipts ->
        eltWriter.writeList(listOfReceipts) {
            txWriter, txReceipt ->
          txReceipt.writeTo(txWriter)
        }
      }
    }
  }

  companion object {

    fun read(bytes: Bytes): ReceiptsMessage {
      return RLP.decodeList(
        bytes
      ) { reader ->
        ReceiptsMessage(
          reader.readLong(),
          reader.readLong(),
          reader.readListContents { listTx -> listTx.readListContents { TransactionReceipt.readFrom(it) } }
        )
      }
    }
  }
}
