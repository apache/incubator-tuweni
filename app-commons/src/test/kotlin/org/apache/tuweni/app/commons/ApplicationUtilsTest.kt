// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.app.commons

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ApplicationUtilsTest {

  @Test
  fun testVersion() {
    assertFalse(ApplicationUtils.version.isBlank())
  }
}
