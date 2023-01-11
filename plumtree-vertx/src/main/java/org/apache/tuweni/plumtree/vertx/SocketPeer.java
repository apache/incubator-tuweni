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
    return socket.localAddress().toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    SocketPeer that = (SocketPeer) o;
    return Objects.equals(socket.localAddress(), that.socket.localAddress());
  }

  @Override
  public int hashCode() {
    return Objects.hash(socket.localAddress().toString());
  }

  @Override
  public int compareTo(@NotNull Peer o) {
    return socket.localAddress().toString().compareTo(((SocketPeer) o).socket.localAddress().toString());
  }
}
