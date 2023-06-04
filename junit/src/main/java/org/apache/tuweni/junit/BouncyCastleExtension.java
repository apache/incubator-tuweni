// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.junit;

import org.apache.tuweni.crypto.blake2bf.TuweniProvider;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * A junit5 extension, that installs a BouncyCastle security provider.
 *
 */
public class BouncyCastleExtension implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    Security.addProvider(new BouncyCastleProvider());
    Security.addProvider(new TuweniProvider());
  }
}
