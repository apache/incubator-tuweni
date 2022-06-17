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
package org.apache.tuweni.jsonrpc.methods

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.StringOrLong
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class NetTest {

  @Test
  fun testNetCheck() = runBlocking {
    val net = registerNet("2", true, { 2 })
    assertEquals("2", net["net_version"]?.invoke(JSONRPCRequest(StringOrLong(1), "", arrayOf()))?.result)
    assertEquals(true, net["net_listening"]?.invoke(JSONRPCRequest(StringOrLong(1), "", arrayOf()))?.result)
    assertEquals("0x2", net["net_peerCount"]?.invoke(JSONRPCRequest(StringOrLong(1), "", arrayOf()))?.result)
  }
}
