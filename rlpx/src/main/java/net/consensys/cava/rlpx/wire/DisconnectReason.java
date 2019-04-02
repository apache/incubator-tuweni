/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.rlpx.wire;

/**
 * Enumeration of all reasons disconnect may happen.
 *
 */
public enum DisconnectReason {
  REQUESTED(0),
  TCP_ERROR(1),
  PROTOCOL_BREACH(2),
  USELESS_PEER(3),
  TOO_MANY_PEERS(4),
  ALREADY_CONNECTED(5),
  INCOMPATIBLE_DEVP2P_VERSION(6),
  NULL_NODE_IDENTITY_RECEIVED(7),
  CLIENT_QUITTING(8),
  UNEXPECTED_IDENTITY(9),
  CONNECTED_TO_SELF(10),
  TIMEOUT(11),
  SUBPROTOCOL_REASON(16);

  DisconnectReason(int code) {
    this.code = code;
  }

  public final int code;
}
