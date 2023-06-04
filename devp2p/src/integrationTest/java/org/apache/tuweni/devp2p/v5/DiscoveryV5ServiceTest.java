// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.v5;

import org.apache.tuweni.crypto.SECP256K1;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;

import java.net.InetSocketAddress;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

@Timeout(10)
@ExtendWith({BouncyCastleExtension.class, VertxExtension.class})
class DiscoveryV5ServiceTest {

  @Test
  void testStartAndStop(@VertxInstance Vertx vertx) throws InterruptedException {
    DiscoveryV5Service service =
        DiscoveryService.open(
            vertx, SECP256K1.KeyPair.random(), 0, new InetSocketAddress("localhost", 34555));
    service.startAsync().join();
    service.terminateAsync().join();
  }
}
