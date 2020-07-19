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
import org.apache.tuweni.devp2p.v5.UdpMessage.Companion.RANDOM_DATA_LENGTH

internal class RandomMessage(
  val authTag: Bytes = UdpMessage.authTag(),
  val data: Bytes = randomData()
) : UdpMessage {

  companion object {
    fun randomData(): Bytes = Bytes.random(RANDOM_DATA_LENGTH)

    fun create(authTag: Bytes, content: Bytes = randomData()): RandomMessage {
      return RandomMessage(authTag, content)
    }
  }

  override fun getMessageType(): Bytes {
    throw UnsupportedOperationException("Message type unsupported for random messages")
  }

  override fun encode(): Bytes {
    return data
  }
}
