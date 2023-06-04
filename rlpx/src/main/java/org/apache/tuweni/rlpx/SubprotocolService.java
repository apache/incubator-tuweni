// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.rlpx.wire.DisconnectReason;
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier;
import org.apache.tuweni.rlpx.wire.WireConnection;

public interface SubprotocolService {
  /**
   * Sends a wire message to a peer.
   *
   * @param subProtocolIdentifier the identifier of the subprotocol this message is part of
   * @param messageType the type of the message according to the subprotocol
   * @param connection the connection.
   * @param message the message, addressed to a connection.
   */
  void send(
      SubProtocolIdentifier subProtocolIdentifier,
      int messageType,
      WireConnection connection,
      Bytes message);

  /**
   * Sends a message to the peer explaining that we are about to disconnect.
   *
   * @param connection the connection to target
   * @param reason the reason for disconnection
   */
  void disconnect(WireConnection connection, DisconnectReason reason);

  /**
   * Gets the wire connections repository associated with this service.
   *
   * @return the repository of wire connections associated with this service.
   */
  WireConnectionRepository repository();
}
