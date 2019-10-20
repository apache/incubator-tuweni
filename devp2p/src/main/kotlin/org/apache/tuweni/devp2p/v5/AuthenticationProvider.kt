/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.misc.AuthHeader
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters
import org.apache.tuweni.devp2p.v5.misc.SessionKey

/**
 * Module for securing messages communications. It creates required parameters for peers handshake execution.
 * All session keys information is located here, which are used for message encryption/decryption
 */
interface AuthenticationProvider {

  /**
   * Creates authentication header to initialize handshake process. As a result it creates an authentication
   * header to include to udp message.
   *
   * @param handshakeParams parameters for authentication header creation
   *
   * @return authentication header for handshake initialization
   */
  fun authenticate(handshakeParams: HandshakeInitParameters): AuthHeader

  /**
   * Verifies, that incoming authentication header is valid via decoding authorization response and checking
   * nonce signature. In case if everything is valid, it creates and stores session key
   *
   * @param senderNodeId sender node identifier
   * @param authHeader authentication header for verification
   */
  fun finalizeHandshake(senderNodeId: Bytes, authHeader: AuthHeader)

  /**
   * Provides session key by node identifier
   *
   * @param nodeId node identifier
   *
   * @return session key for message encryption/decryption
   */
  fun findSessionKey(nodeId: String): SessionKey?

  /**
   * Persists session key by node identifier
   *
   * @param nodeId node identifier
   * @param sessionKey session key
   */
  fun setSessionKey(nodeId: String, sessionKey: SessionKey)
}
