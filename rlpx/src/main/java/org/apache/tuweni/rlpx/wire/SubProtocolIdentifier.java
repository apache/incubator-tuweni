// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlpx.wire;


import static java.util.Objects.requireNonNull;

/**
 * Identifier of a subprotocol, comprised of a name and version.
 */
public interface SubProtocolIdentifier {

  static SubProtocolIdentifier of(String name, int version) {
    return of(name, version, 0);
  }

  static SubProtocolIdentifier of(String name, int version, int range) {
    requireNonNull(name);
    return new DefaultSubProtocolIdentifier(name, version, range);
  }

  /**
   * Provides the subprotocol name
   * 
   * @return the name of the subprotocol
   */
  String name();

  /**
   * Provides the subprotocol version
   * 
   * @return the version of the subprotocol
   */
  int version();

  /**
   * Provides the length of the range of message types supported by the subprotocol for a given version
   *
   * @return the length of the range of message types supported by the subprotocol for a given version
   */
  int versionRange();
}
