// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;

import java.util.Collections;

import org.junit.jupiter.api.Test;

class HelloMessageTest {

  @Test
  void p2pVersion() {
    HelloMessage msg =
        HelloMessage.create(
            Bytes.fromHexString("deadbeef"), 10000, 3, "blah", Collections.emptyList());
    HelloMessage msgRead = HelloMessage.read(msg.toBytes());
    assertEquals(3, msgRead.p2pVersion());
  }

  @Test
  void nodeId() {
    HelloMessage msg =
        HelloMessage.create(
            Bytes.fromHexString("deadbeef"), 10000, 3, "blah", Collections.emptyList());
    HelloMessage msgRead = HelloMessage.read(msg.toBytes());
    assertEquals(Bytes.fromHexString("deadbeef"), msgRead.nodeId());
  }

  @Test
  void clientId() {
    HelloMessage msg =
        HelloMessage.create(
            Bytes.fromHexString("deadbeef"), 10000, 3, "foofoo", Collections.emptyList());
    HelloMessage msgRead = HelloMessage.read(msg.toBytes());
    assertEquals("foofoo", msgRead.clientId());
  }
}
