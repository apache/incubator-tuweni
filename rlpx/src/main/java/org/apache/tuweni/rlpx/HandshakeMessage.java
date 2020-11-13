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

import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.SECP256K1;

/**
 * Contents of a message sent as part of a RLPx handshake.
 */
public interface HandshakeMessage {

  /**
   * Provides the ephemeral public key
   * 
   * @return the ephemeral public key included in the response
   */
  public SECP256K1.PublicKey ephemeralPublicKey();

  /**
   * Provides the nonce
   * 
   * @return the response nonce
   */
  public Bytes32 nonce();
}
