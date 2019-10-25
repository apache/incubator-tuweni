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

class WhoAreYouMessageTest {

  @Test
  fun encodeCreatesValidBytesSequence() {
    val expectedEncodingResult =
      "0xEF8C05D038D54B1ACB9A2A83C480A0C3B548CA063DA57BC9DE93340360AF32815FC8D0B2F053B3CB7918ABBB291A5180"

    val authTag = Bytes.fromHexString("0x05D038D54B1ACB9A2A83C480")
    val nonce = Bytes.fromHexString("0xC3B548CA063DA57BC9DE93340360AF32815FC8D0B2F053B3CB7918ABBB291A51")
    val message = WhoAreYouMessage(authTag, nonce)

    val encodingResult = message.encode()
    assert(encodingResult.toHexString() == expectedEncodingResult)

    val decodingResult = WhoAreYouMessage.create(encodingResult)

    assert(decodingResult.authTag == authTag)
    assert(decodingResult.idNonce == nonce)
    assert(decodingResult.enrSeq == 0L)
  }
}
