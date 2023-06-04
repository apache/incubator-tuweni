// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;

import org.junit.jupiter.api.Test;

class DisconnectMessageTest {

  @Test
  void testBytesRoundtrip() {
    DisconnectMessage msg = new DisconnectMessage(4);
    Bytes toBytes = msg.toBytes();
    DisconnectMessage read = DisconnectMessage.read(toBytes);
    assertEquals(msg.messageType(), read.messageType());
    assertEquals(msg.reason(), read.reason());
  }

  @Test
  void testEmptyDisconnect() {
    DisconnectMessage read = DisconnectMessage.read(Bytes.EMPTY);
    assertEquals(1, read.messageType());
    assertEquals(DisconnectReason.NOT_PROVIDED.code, read.reason());
  }

  @Test
  void testWeirdValue() {
    DisconnectMessage read = DisconnectMessage.read(Bytes.fromHexString("0x01adbeef"));
    assertEquals(1, read.messageType());
    assertEquals(DisconnectReason.NOT_PROVIDED.code, read.reason());
  }

  @Test
  void testEmptyList() {
    DisconnectMessage read = DisconnectMessage.read(Bytes.fromHexString("0xc0"));
    assertEquals(1, read.messageType());
    assertEquals(DisconnectReason.NOT_PROVIDED.code, read.reason());
  }
}
