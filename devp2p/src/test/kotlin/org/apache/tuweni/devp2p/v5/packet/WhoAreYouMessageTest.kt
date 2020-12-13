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
import org.apache.tuweni.devp2p.v5.WhoAreYouMessage
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
class WhoAreYouMessageTest {

  @Test
  fun decodeSelf() {
    val bytes =
      Bytes.fromHexString(
        "0x282E641D415A892C05FD03F0AE716BDD92D1569116FDC7C7D3DB39AC5F79B0F7EF8C" +
          "E56EDC7BB967899B4C48EEA6A0E838C9091B71DADB98C59508306275AE37A1916EF2517E77CFE09FA006909FE880"
      )
    WhoAreYouMessage.create(magic = bytes.slice(0, 32), content = bytes.slice(32))
  }
}
