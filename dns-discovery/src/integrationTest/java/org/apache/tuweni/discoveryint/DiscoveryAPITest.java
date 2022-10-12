/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
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
    DNSVisitor visitor = enr -> {
      nodes.add(enr);
      return false;
    };
    AsyncCompletion completion = resolver
        .visitTreeAsync(
            "enrtree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@all.goerli.ethdisco.net",
            visitor);
    completion.join();
    assertTrue(nodes.size() > 0);
  }
}
