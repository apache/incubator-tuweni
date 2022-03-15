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
package org.apache.tuweni.plumtree;

import org.apache.tuweni.bytes.Bytes;

import javax.annotation.Nullable;

/**
 * Interface to sending messages to other peers.
 */
public interface MessageSender {

  /**
   * Types of verbs supported by the dialect
   */
  enum Verb {
    /**
     * Indicates we have a message
     */
    IHAVE,
    /**
     * Ask to be added back to eager peers
     */
    GRAFT,
    /**
     * Ask to be removed from eager peers
     */
    PRUNE,
    /**
     * Gossip a message
     */
    GOSSIP,
    /**
     * Send a direct message
     */
    SEND
  }

  /**
   * Sends bytes to a peer.
   * 
   * @param verb the type of message
   * @param attributes the attributes of message
   * @param peer the target of the message
   * @param hash the hash of the message
   * @param payload the bytes to send
   */
  void sendMessage(Verb verb, String attributes, Peer peer, Bytes hash, @Nullable Bytes payload);

}
