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
package net.consensys.cava.plumtree;

import net.consensys.cava.bytes.Bytes;

import javax.annotation.Nullable;

/**
 * Interface to sending messages to other peers.
 */
public interface MessageSender {

  /**
   * Types of message supported by the dialect
   */
  enum Verb {
    IHAVE, GRAFT, PRUNE, GOSSIP
  }

  /**
   * Sends bytes to a peer.
   * 
   * @param verb the type of message
   * @param peer the target of the message
   * @param payload the bytes to send
   */
  void sendMessage(Verb verb, Peer peer, @Nullable Bytes payload);
}
