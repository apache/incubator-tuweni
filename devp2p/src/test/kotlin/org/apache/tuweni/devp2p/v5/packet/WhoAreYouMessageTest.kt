// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
          "E56EDC7BB967899B4C48EEA6A0E838C9091B71DADB98C59508306275AE37A1916EF2517E77CFE09FA006909FE880",
      )
    WhoAreYouMessage.create(magic = bytes.slice(0, 32), content = bytes.slice(32))
  }
}
