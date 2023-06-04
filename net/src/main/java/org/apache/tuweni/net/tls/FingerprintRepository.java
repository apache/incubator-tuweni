// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.net.tls;

import org.apache.tuweni.bytes.Bytes;

/** Repository of remote peer fingerprints. */
public interface FingerprintRepository {

  /**
   * Checks whether the identifier of the remote peer is present in the repository.
   *
   * @param identifier the identifier of a remote peer
   * @return true if the remote peer identifier is present in the repository
   */
  boolean contains(String identifier);

  /**
   * Checks whether the identifier of the remote peer is present in the repository, and its
   * fingerprint matches the fingerprint present.
   *
   * @param identifier the identifier of a remote peer
   * @param fingerprint the fingerprint of a remote peer
   * @return true if there is a peer in the repository associated with that fingerprint
   */
  boolean contains(String identifier, Bytes fingerprint);

  /**
   * Adds the fingerprint of a remote peer to the repository.
   *
   * @param identifier the identifier of a remote peer
   * @param fingerprint the fingerprint of a remote peer
   */
  void addFingerprint(String identifier, Bytes fingerprint);
}
