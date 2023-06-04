// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.discoveryint;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.devp2p.EthereumNodeRecord;
import org.apache.tuweni.discovery.DNSResolver;
import org.apache.tuweni.discovery.DNSVisitor;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;

import java.util.ArrayList;
import java.util.List;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith({BouncyCastleExtension.class, VertxExtension.class})
class DiscoveryAPITest {

  @Disabled("too unstable on CI")
  @Test
  void resolveNames(@VertxInstance Vertx vertx) throws Exception {
    DNSResolver resolver = new DNSResolver(vertx);
    List<EthereumNodeRecord> nodes = new ArrayList<>();
    DNSVisitor visitor =
        enr -> {
          nodes.add(enr);
          return false;
        };
    AsyncCompletion completion =
        resolver.visitTreeAsync(
            "enrtree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@all.goerli.ethdisco.net",
            visitor);
    completion.join();
    assertTrue(nodes.size() > 0);
  }
}
