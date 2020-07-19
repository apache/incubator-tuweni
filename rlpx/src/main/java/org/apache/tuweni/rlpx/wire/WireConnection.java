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


import org.apache.tuweni.crypto.SECP256K1;

/**
 * A stateful connection between two peers under the Devp2p wire protocol.
 */
public interface WireConnection {

  /**
   * Returns true if the connection supports the subprotocol
   * 
   * @param subProtocolIdentifier the subprotocol identifier
   * @return true if the subprotocol is supported
   */
  boolean supports(SubProtocolIdentifier subProtocolIdentifier);

  /**
   * Whether the peer asked to disconnect
   * 
   * @return true if the peer asked to disconnect
   */
  boolean isDisconnectReceived();

  /**
   * Whether this service asked to disconnect
   * 
   * @return true if this service asked to disconnect
   */
  boolean isDisconnectRequested();

  /**
   * If the connection is disconnected, the reason for which the connection is disconnected.
   * 
   * @return the disconnect reason if it happened.
   */
  DisconnectReason getDisconnectReason();

  /**
   * Network interface associated with the wire connection
   * 
   * @return the peer's host network interface
   */
  String peerHost();

  /**
   * Port associated with the wire connection
   * 
   * @return the peer port value
   */
  int peerPort();

  /**
   * Public key of the peer associated with the wire connection
   * 
   * @return the peer public key
   */
  SECP256K1.PublicKey peerPublicKey();

  /**
   * Provides the identity of the connection
   * 
   * @return the connection identity as an enode address.
   */
  default String uri() {
    return "enode://" + peerPublicKey().toHexString() + "@" + peerHost() + ":" + peerPort();
  }
}
