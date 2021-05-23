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

import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.methodNotFound
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
class MethodsHandlerTest {

  @Test
  fun testMissingMethod() {
    val methodsRouter = MethodsRouter(emptyMap())
    assertEquals(methodNotFound, methodsRouter.handleRequest(JSONRPCRequest(1, "web3_sha3", arrayOf("0xdeadbeef"))))
  }

  @Test
  fun testRouteMethod() {
    val methodsRouter = MethodsRouter(mapOf(Pair("web3_sha3", ::sha3)))
    assertEquals(JSONRPCResponse(1, result = "0xd4fd4e189132273036449fc9e11198c739161b4c0116a9a2dccdfa1c492006f1"), methodsRouter.handleRequest(JSONRPCRequest(1, "web3_sha3", arrayOf("0xdeadbeef"))))
  }
}
