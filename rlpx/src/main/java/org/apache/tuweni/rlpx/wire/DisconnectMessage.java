/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
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
