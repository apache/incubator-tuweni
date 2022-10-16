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
