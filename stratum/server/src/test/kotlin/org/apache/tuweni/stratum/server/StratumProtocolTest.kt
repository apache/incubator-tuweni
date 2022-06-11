/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.stratum.server

import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes32
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StratumProtocolTest {

  @Test
  fun testStratum1CanHandle() {
    val protocol = Stratum1Protocol(
      "", submitCallback = { true }, seedSupplier = Bytes32::random,
      coroutineContext = Dispatchers.Default
    )
    val conn = StratumConnection(emptyArray(), {}, {})
    assertFalse(protocol.canHandle("", conn))
    assertFalse(protocol.canHandle("\"mining.subscribe", conn))
    assertFalse(protocol.canHandle("{\"method\": \"mining.subscribe\"}", conn))
    assertTrue(protocol.canHandle("{\"method\": \"mining.subscribe\", \"id\": 1, \"params\": []}", conn))
  }

  @Test
  fun testEthProxyCanHandle() {
    val protocol = Stratum1EthProxyProtocol(
      submitCallback = { true },
      seedSupplier = Bytes32::random,
      hashrateCallback = { _, _ -> true },
      Dispatchers.Default,
    )
    val conn = StratumConnection(emptyArray(), {}, {})
    assertFalse(protocol.canHandle("", conn))
    assertFalse(protocol.canHandle("\"eth_submitLogin", conn))
    assertFalse(protocol.canHandle("{\"method\": \"eth_submitLogin\"}", conn))
    assertTrue(protocol.canHandle("{\"method\": \"eth_submitLogin\", \"id\": 1, \"params\": []}", conn))
  }
}
