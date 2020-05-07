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

import org.apache.tuweni.devp2p.EthereumNodeRecord;
import org.apache.tuweni.discovery.DNSResolver;
import org.apache.tuweni.discovery.DNSVisitor;
import org.apache.tuweni.junit.BouncyCastleExtension;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class DiscoveryAPITest {

  @Test
  void resolveNames() {
    DNSResolver resolver = new DNSResolver();
    List<EthereumNodeRecord> nodes = new ArrayList<>();
    DNSVisitor visitor = enr -> {
      nodes.add(enr);
      return false;
    };
    resolver
        .visitTree("enrtree://AKA3AM6LPBYEUDMVNU3BSVQJ5AD45Y7YPOHJLEF6W26QOE4VTUDPE@all.mainnet.ethdisco.net", visitor);
    assertTrue(nodes.size() > 0);
  }
}
