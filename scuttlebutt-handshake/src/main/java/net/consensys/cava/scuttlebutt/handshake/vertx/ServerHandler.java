/*
 * Copyright 2019 ConsenSys AG.
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
package net.consensys.cava.scuttlebutt.handshake.vertx;

import net.consensys.cava.bytes.Bytes;

/**
 * Handler managing a stream over SecureScuttlebutt originating from the Vert.x server
 */
public interface ServerHandler {

  /**
   * This method is called by the server when a new message arrives from the client.
   * 
   * @param message the new message after box decoding.
   */
  void receivedMessage(Bytes message);

  /**
   * Notifies the handler the stream is no longer active.
   */
  void streamClosed();
}
