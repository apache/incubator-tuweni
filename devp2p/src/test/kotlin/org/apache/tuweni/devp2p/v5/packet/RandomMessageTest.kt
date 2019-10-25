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
import org.junit.jupiter.api.Test

class RandomMessageTest {

  @Test
  fun encodeCreatesValidBytesSequence() {
    val expectedEncodingResult =
      "0xB53CCF732982B8E950836D1E02898C8B38CFDBFDF86BC65C8826506B454E14618EA73612A0F5582C130FF666"

    val data = Bytes.fromHexString(expectedEncodingResult)
    val message = RandomMessage(data)

    val encodingResult = message.encode()
    assert(encodingResult.toHexString() == expectedEncodingResult)

    val decodingResult = RandomMessage.create(encodingResult)

    assert(decodingResult.data == data)
  }
}
