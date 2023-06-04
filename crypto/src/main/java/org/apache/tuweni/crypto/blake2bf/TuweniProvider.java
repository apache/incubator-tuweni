// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.blake2bf;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;

/** Bouncy Castle Security Provider for specific Apache Tuweni message digests. */
public final class TuweniProvider extends Provider {

  private static final String info = "Tuweni Security Provider v1.0";

  public static final String PROVIDER_NAME = "Tuweni";

  @SuppressWarnings({"unchecked", "removal"})
  public TuweniProvider() {
    super(PROVIDER_NAME, "1.0", info);
    AccessController.doPrivileged(
        (PrivilegedAction)
            () -> {
              put("MessageDigest.Blake2bf", Blake2bfMessageDigest.class.getName());
              return null;
            });
  }
}
