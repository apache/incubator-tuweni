// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.junit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.security.Security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class BouncyCastleExtensionTest {

  @Test
  void testExtensionLoaded() {
    assertNotNull(Security.getProvider("BC"));
  }
}
