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

/**
 * Enumeration of all reasons disconnect may happen.
 *
 */
public enum DisconnectReason {

  REQUESTED(0, "Requested"),
  TCP_ERROR(1, "TCP Error"),
  PROTOCOL_BREACH(2, "Protocol breach"),
  USELESS_PEER(3, "Useless peer"),
  TOO_MANY_PEERS(4, "Too many peers"),
  ALREADY_CONNECTED(5, "Already connected"),
  INCOMPATIBLE_DEVP2P_VERSION(6, "Incompatible devp2p version"),
  NULL_NODE_IDENTITY_RECEIVED(7, "Null node identity received"),
  CLIENT_QUITTING(8, "Client quitting"),
  UNEXPECTED_IDENTITY(9, "Unexpected identity"),
  CONNECTED_TO_SELF(10, "Connected to self"),
  TIMEOUT(11, "Timeout"),
  SUBPROTOCOL_REASON(16, "Subprotocol reason");

  public static DisconnectReason valueOf(int reason) {
    switch (reason) {
      case 0:
        return REQUESTED;
      case 1:
        return TCP_ERROR;
      case 2:
        return PROTOCOL_BREACH;
      case 3:
        return USELESS_PEER;
      case 4:
        return TOO_MANY_PEERS;
      case 5:
        return ALREADY_CONNECTED;
      case 6:
        return INCOMPATIBLE_DEVP2P_VERSION;
      case 7:
        return NULL_NODE_IDENTITY_RECEIVED;
      case 8:
        return CLIENT_QUITTING;
      case 9:
        return UNEXPECTED_IDENTITY;
      case 10:
        return CONNECTED_TO_SELF;
      case 11:
        return TIMEOUT;
      case 16:
        return SUBPROTOCOL_REASON;
      default:
        throw new IllegalArgumentException("Invalid reason " + reason);
    }
  }

  DisconnectReason(int code, String text) {
    this.code = code;
    this.text = text;
  }

  public final int code;
  public final String text;
}
