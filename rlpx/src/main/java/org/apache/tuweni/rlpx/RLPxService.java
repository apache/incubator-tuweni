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
package org.apache.tuweni.rlpx;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.rlpx.wire.DisconnectReason;
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier;

import java.net.InetSocketAddress;

/**
 * Service allowing connections to remote peers over RLPx connections.
 */
public interface RLPxService {

  /**
   * Connects to a remote peer
   *
   * @param peerPublicKey the peer public key
   * @param peerAddress the peer host and port
   * @return a handle that completes if the peer connects successfully, providing the connection ID.
   */
  AsyncResult<String> connectTo(SECP256K1.PublicKey peerPublicKey, InetSocketAddress peerAddress);


  /**
   * Starts the service.
   * 
   * @return a future handler tracking starting the service.
   */
  AsyncCompletion start();

  /**
   * Stops the service.
   * 
   * @return a future handler tracking stopping the service.
   */
  AsyncCompletion stop();

  /**
   * Sends a wire message to a peer.
   *
   * @param subProtocolIdentifier the identifier of the subprotocol this message is part of
   * @param messageType the type of the message according to the subprotocol
   * @param connectionId the identifier of the connection.
   * @param message the message, addressed to a connection.
   */
  void send(SubProtocolIdentifier subProtocolIdentifier, int messageType, String connectionId, Bytes message);

  /**
   * Sends a wire message to all connected peers.
   *
   * @param subProtocolIdentifier the identifier of the subprotocol this message is part ofs
   * @param messageType the type of the message according to the subprotocol
   * @param message the message to broadcast.
   */
  void broadcast(SubProtocolIdentifier subProtocolIdentifier, int messageType, Bytes message);

  /**
   * Sends a message to the peer explaining that we are about to disconnect.
   *
   * @param connectionId the identifier of the connection to target
   * @param reason the reason for disconnection
   */
  void disconnect(String connectionId, DisconnectReason reason);

  /**
   * Gets the wire connections repository associated with this service.
   *
   * @return the repository of wire connections associated with this service.
   */
  WireConnectionRepository repository();
}
