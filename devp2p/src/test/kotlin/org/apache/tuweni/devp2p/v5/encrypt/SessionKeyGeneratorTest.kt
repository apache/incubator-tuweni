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
package org.apache.tuweni.devp2p.v5.encrypt

import org.apache.tuweni.bytes.Bytes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SessionKeyGeneratorTest {

  @Test
  fun generateCreatesSessionKey() {
    val expectedAuthRespKey = Bytes.fromHexString("0xDC999F3F7EF11907F6762497476117C9")
    val expectedInitiatorKey = Bytes.fromHexString("0xBBBE757DCE9687BBE5E90CBF9C776163")
    val expectedRecipientKey = Bytes.fromHexString("0xE83FC3ED3B32DEE7D81D706FECA6174F")

    val srcNodeId = Bytes.fromHexString("0x9CE70B8F317791EB4E775FF9314B9B7B2CD01D90FF5D0E1979B2EBEB92DCB48D")
    val destNodeId = Bytes.fromHexString("0x0B1E82724DB4D17089EF64A441A2C367683EAC448E6AB7F6F8B3094D2B1B2229")
    val secret = Bytes.fromHexString("0xAB285AD41C712A917DAC83DE8AAD963285067ED84BAC37052A32BB74DCC75AA5")
    val idNonce = Bytes.fromHexString("0x630222D6CD1253BF40CB800F230759F117EC1890CD76792135BBC4D7AAD0B4C1")

    val result = SessionKeyGenerator.generate(srcNodeId, destNodeId, secret, idNonce)

    assertEquals(result.authRespKey, expectedAuthRespKey)
    assertEquals(result.initiatorKey, expectedInitiatorKey)
    assertEquals(result.recipientKey, expectedRecipientKey)
  }
}
