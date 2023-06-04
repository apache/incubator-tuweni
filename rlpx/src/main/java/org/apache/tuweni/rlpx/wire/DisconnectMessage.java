// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx.wire;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.rlp.RLP;
import org.apache.tuweni.rlp.RLPException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DisconnectMessage implements WireProtocolMessage {

  private final static Logger logger = LoggerFactory.getLogger(DisconnectMessage.class);

  static DisconnectMessage read(Bytes data) {
    try {
      return RLP.decode(data, (reader) -> {
        if (reader.nextIsList()) {
          return reader.readList((source) -> new DisconnectMessage(source.readInt()));
        }
        return new DisconnectMessage(DisconnectReason.NOT_PROVIDED);
      });
    } catch (RLPException e) {
      logger.warn("Error reading disconnect message " + data.toHexString(), e);
      return new DisconnectMessage(DisconnectReason.NOT_PROVIDED);
    }
  }

  private final DisconnectReason reason;

  DisconnectMessage(int reason) {
    this(DisconnectReason.valueOf(reason));
  }

  DisconnectMessage(DisconnectReason reason) {
    this.reason = reason;
  }

  @Override
  public Bytes toBytes() {
    return RLP.encodeList(writer -> writer.writeInt(reason.code));
  }

  @Override
  public int messageType() {
    return 1;
  }

  int reason() {
    return reason.code;
  }

  DisconnectReason disconnectReason() {
    return reason;
  }

  @Override
  public String toString() {
    return "DisconnectMessage reason=" + reason.text;
  }
}
