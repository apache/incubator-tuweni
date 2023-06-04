// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.proxy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MessagesTest {

  @Test
  fun testStatusRoundtrip() {
    val status = StatusMessage(listOf("foo", "bar"))
    val recreated = StatusMessage.decode(status.toRLP())
    assertEquals(status, recreated)
  }
}
