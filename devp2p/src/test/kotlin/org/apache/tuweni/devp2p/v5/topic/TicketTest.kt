// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.v5.topic

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TicketTest {

  @Test
  fun roundtrip() {
    val ticket =
      Ticket(Bytes.wrap("hello world".toByteArray()), Bytes32.random(), "127.0.0.1", 0L, 0L, 0L)
    val key = Bytes.random(16)
    val encrypted = ticket.encrypt(key)
    assertEquals(Ticket.decrypt(encrypted, key), ticket)
  }
}
