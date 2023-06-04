// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx;

import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.rlpx.wire.SubProtocolClient;
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier;
import org.apache.tuweni.rlpx.wire.WireConnection;

import java.net.InetSocketAddress;

/** Service allowing connections to remote peers over RLPx connections. */
public interface RLPxService extends SubprotocolService {

  /**
   * Connects to a remote peer.
   *
   * @param peerPublicKey the peer public key
   * @param peerAddress the peer host and port
   * @return a handle that completes when the connection has occurred. The connection itself may be
   *     marked as disconnected as it was deemed invalid or useless.
   */
  AsyncResult<WireConnection> connectTo(
      SECP256K1.PublicKey peerPublicKey, InetSocketAddress peerAddress);

  /**
   * Provides the actual port in use.
   *
   * @return the port used by the server
   * @throws IllegalStateException if the service is not started
   */
  int actualPort();

  /**
   * Provides the actual socket address (network interface and port) in use.
   *
   * @return the socket address used by the server
   * @throws IllegalStateException if the service is not started
   */
  InetSocketAddress actualSocketAddress();

  /**
   * Provides the advertised port.
   *
   * @return the port advertised by the server
   * @throws IllegalStateException if the service is not started
   */
  int advertisedPort();

  /**
   * Adds a peer public key to the list of peers to keep alive.
   *
   * @param peerPublicKey the peer public key
   */
  boolean addToKeepAliveList(SECP256K1.PublicKey peerPublicKey);

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
   * Gets a subprotocol client associated with the given subprotocol.
   *
   * @param subProtocolIdentifier the subprotocol identifier
   * @return the client of the subprotocol.
   */
  SubProtocolClient getClient(SubProtocolIdentifier subProtocolIdentifier);
}
