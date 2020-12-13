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
        DiscoveryService.open(vertx, SECP256K1.KeyPair.random(), 0, new InetSocketAddress("localhost", 34555));
    service.startAsync().join();
    service.terminateAsync().join();
  }
}
