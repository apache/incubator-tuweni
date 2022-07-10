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
  /**
   * Explicitly requested to disconnect
   */
  REQUESTED(0, "Requested"),
  /**
   * Error in the TCP transport
   */
  TCP_ERROR(1, "TCP Error"),
  /**
   * Error in the protocol or subprotocol
   */
  PROTOCOL_BREACH(2, "Protocol breach"),
  /**
   * Peer is useless, it has no matching subprotocols
   */
  USELESS_PEER(3, "Useless peer"),
  /**
   * We already have enough peers
   */
  TOO_MANY_PEERS(4, "Too many peers"),
  /**
   * We are already connected to this peer
   */
  ALREADY_CONNECTED(5, "Already connected"),
  /**
   * Peer has an incompatible devp2p version
   */
  INCOMPATIBLE_DEVP2P_VERSION(6, "Incompatible devp2p version"),
  /**
   * The peer sent an invalid, null identity
   */
  NULL_NODE_IDENTITY_RECEIVED(7, "Null node identity received"),
  /**
   * The client is exiting
   */
  CLIENT_QUITTING(8, "Client quitting"),
  /**
   * The identity the peer sends doesn't match what we expected
   */
  UNEXPECTED_IDENTITY(9, "Unexpected identity"),
  /**
   * The peer is us
   */
  CONNECTED_TO_SELF(10, "Connected to self"),
  /**
   * We hit a timeout
   */
  TIMEOUT(11, "Timeout"),
  /**
   * A subprotocol has reason to request a disconnection
   */
  SUBPROTOCOL_REASON(16, "Subprotocol reason"),
  /**
   * No reason was provided for the disconnection
   */
  NOT_PROVIDED(99, "No reason provided");

  /**
   * Get a disconnect reason for a number code
   * 
   * @param reason the reason code
   * @return a matching DisconnectReason
   */
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
      case 99:
        return NOT_PROVIDED;
      default:
        throw new IllegalArgumentException("Invalid reason " + reason);
    }
  }

  DisconnectReason(int code, String text) {
    this.code = code;
    this.text = text;
  }

  /**
   * Disconnect reason code
   */
  public final int code;
  /**
   * Disconnect reason text
   */
  public final String text;
}
