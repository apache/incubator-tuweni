// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
