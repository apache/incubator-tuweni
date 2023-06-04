// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.plumtree;

import org.apache.tuweni.bytes.Bytes;

/**
 * Produces an identifiable footprint for a message (generally a hash) that can be passed on to other peers to identify
 * uniquely a message being propagated.
 */
public interface MessageIdentity {

  /**
   * Generates the identity of the message
   * 
   * @param message the message from which to extract an identity
   * @return the identity of the message
   */
  Bytes identity(Bytes message);

}
