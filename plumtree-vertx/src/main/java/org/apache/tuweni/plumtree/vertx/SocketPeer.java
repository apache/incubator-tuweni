// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree.vertx;

import org.apache.tuweni.plumtree.Peer;

import java.util.Objects;

import io.vertx.core.net.NetSocket;
import org.jetbrains.annotations.NotNull;

/**
 * Vert.x gossip peer associated with a socket
 */
final class SocketPeer implements Peer {

  private final NetSocket socket;

  SocketPeer(NetSocket socket) {
    this.socket = socket;
  }

  NetSocket socket() {
    return socket;
  }

  @Override
  public String toString() {
    return socket.remoteAddress().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    SocketPeer that = (SocketPeer) o;
    return Objects.equals(socket.remoteAddress(), that.socket.remoteAddress());
  }

  @Override
  public int hashCode() {
    return Objects.hash(socket.remoteAddress().toString());
  }

  @Override
  public int compareTo(@NotNull Peer o) {
    return socket.remoteAddress().toString().compareTo(((SocketPeer) o).socket.remoteAddress().toString());
  }
}
