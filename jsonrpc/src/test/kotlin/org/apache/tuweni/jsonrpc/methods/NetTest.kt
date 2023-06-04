// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
