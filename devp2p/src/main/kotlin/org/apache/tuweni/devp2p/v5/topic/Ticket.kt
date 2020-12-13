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
package org.apache.tuweni.devp2p.v5.topic

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.encrypt.AES128GCM
import org.apache.tuweni.rlp.RLP
import java.net.InetAddress

internal data class Ticket(
  val topic: Bytes,
  val srcNodeId: Bytes,
  val srcIp: String,
  val requestTime: Long,
  val waitTime: Long,
  val cumTime: Long
) {

  companion object {
    private const val ZERO_NONCE_SIZE: Int = 12
    internal const val TIME_WINDOW_MS: Int = 10000 // 10 seconds
    internal const val TICKET_INVALID_MSG = "Ticket is invalid"

    fun create(content: Bytes): Ticket {
      return RLP.decodeList(content) { reader ->
        val topic = reader.readValue()
        val srcNodeId = reader.readValue()
        val srcIp = InetAddress.getByAddress(reader.readValue().toArray())
        val requestTime = reader.readLong()
        val waitTime = reader.readLong()
        val cumTime = reader.readLong()
        return@decodeList Ticket(topic, srcNodeId, srcIp.hostAddress, requestTime, waitTime, cumTime)
      }
    }

    fun decrypt(encrypted: Bytes, key: Bytes): Ticket {
      val decrypted = AES128GCM.decrypt(key, Bytes.wrap(ByteArray(ZERO_NONCE_SIZE)), encrypted, Bytes.EMPTY)
      return create(decrypted)
    }
  }

  fun encode(): Bytes {
    return RLP.encodeList { writer ->
      writer.writeValue(topic)
      writer.writeValue(srcNodeId)
      writer.writeValue(Bytes.wrap(InetAddress.getByName(srcIp).address))
      writer.writeLong(requestTime)
      writer.writeLong(waitTime)
      writer.writeLong(cumTime)
    }
  }

  fun encrypt(key: Bytes): Bytes {
    val ticketBytes = encode()
    return AES128GCM.encrypt(key, Bytes.wrap(ByteArray(ZERO_NONCE_SIZE)), ticketBytes, Bytes.EMPTY)
  }

  fun validate(
    srcNodeId: Bytes,
    srcIp: String,
    now: Long,
    topic: Bytes
  ) {
    require(this.srcNodeId == srcNodeId) { TICKET_INVALID_MSG }
    require(this.srcIp == srcIp) { TICKET_INVALID_MSG }
    require(this.topic == topic) { TICKET_INVALID_MSG }
    val windowStart = this.requestTime + this.waitTime
    require(now >= windowStart && now <= windowStart + TIME_WINDOW_MS) { TICKET_INVALID_MSG }
  }
}
