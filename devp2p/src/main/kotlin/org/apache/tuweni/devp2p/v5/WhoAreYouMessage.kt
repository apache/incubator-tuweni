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
package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.rlp.RLP

internal class WhoAreYouMessage(
  val authTag: Bytes = UdpMessage.authTag(),
  val idNonce: Bytes = UdpMessage.idNonce(),
  val enrSeq: Long = 0
) : UdpMessage {

  companion object {
    fun create(content: Bytes): WhoAreYouMessage {
      return RLP.decodeList(content) { r ->
        val authTag = r.readValue()
        val idNonce = r.readValue()
        val enrSeq = r.readLong()
        return@decodeList WhoAreYouMessage(authTag, idNonce, enrSeq)
      }
    }
  }

  override fun getMessageType(): Bytes {
    throw UnsupportedOperationException("Message type unsupported for whoareyou messages")
  }

  override fun encode(): Bytes {
    return RLP.encodeList { w ->
      w.writeValue(authTag)
      w.writeValue(idNonce)
      w.writeLong(enrSeq)
    }
  }
}
