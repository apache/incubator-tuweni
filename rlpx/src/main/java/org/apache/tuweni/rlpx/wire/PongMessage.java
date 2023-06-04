// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx.wire;

import org.apache.tuweni.bytes.Bytes;

final class PongMessage implements WireProtocolMessage {

  static PongMessage read(Bytes data) {
    return new PongMessage();
  }

  @Override
  public Bytes toBytes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public int messageType() {
    throw new UnsupportedOperationException();
  }
}
