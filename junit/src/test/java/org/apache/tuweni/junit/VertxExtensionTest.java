// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.junit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class VertxExtensionTest {

  @Test
  void test(@VertxInstance Vertx vertx) {
    assertNotNull(vertx);
  }
}
