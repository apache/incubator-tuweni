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

final class DisconnectMessage implements WireProtocolMessage {

  private final int reason;

  DisconnectMessage(DisconnectReason reason) {
    this(reason.code);
  }

  DisconnectMessage(int reason) {
    this.reason = reason;
  }

  static DisconnectMessage read(Bytes data) {
    return RLP.decodeList(data, source -> new DisconnectMessage(source.readInt()));
  }

  @Override
  public Bytes toBytes() {
    return RLP.encodeList(writer -> writer.writeInt(reason));
  }

  @Override
  public int messageType() {
    return 1;
  }

  int reason() {
    return reason;
  }
}
