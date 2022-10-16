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
package org.apache.tuweni.devp2p.v5.packet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.Message
import org.apache.tuweni.devp2p.v5.RandomMessage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class RandomMessageTest {

  @Test
  fun encodeCreatesValidBytesSequence() {
    val expectedEncodingResult =
      "0xb53ccf732982b8e950836d1e02898c8b38cfdbfdf86bc65c8826506b454e14618ea73612a0f5582c130ff666"

    val data = Bytes.fromHexString(expectedEncodingResult)
    val message = RandomMessage(Message.authTag(), data)

    val encodingResult = message.toRLP()
    assertEquals(encodingResult.toHexString(), expectedEncodingResult)

    val decodingResult = RandomMessage.create(Message.authTag(), encodingResult)

    assertEquals(decodingResult.data, data)
  }

  @Test
  fun randomDataGivesRandom44Bytes() {
    val firstResult = RandomMessage.randomData()

    assertEquals(Message.RANDOM_DATA_LENGTH, firstResult.size())

    val secondResult = RandomMessage.randomData()

    assertNotEquals(secondResult, firstResult)
  }
}
