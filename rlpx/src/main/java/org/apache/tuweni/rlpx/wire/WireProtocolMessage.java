// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx.wire;

import org.apache.tuweni.bytes.Bytes;

/**
 * A set of bytes made available to a subprotocol after it has been successfully decrypted.
 */
interface WireProtocolMessage {


  /**
   * Provides the message payload
   * 
   * @return the payload of the wire message, ready for consumption.
   */
  Bytes toBytes();

  /**
   * Provides the message code
   * 
   * @return the code associated with the message type according to the subprotocol.
   */
  int messageType();
}
