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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.JSONRPCResponse
import org.apache.tuweni.eth.invalidParams

fun registerWeb3(clientVersion: String): Map<String, suspend (JSONRPCRequest) -> JSONRPCResponse> {
  val version = ConstantStringResult(clientVersion)
  return mapOf(Pair("web3_sha3", ::sha3), Pair("web3_clientVersion", version::handle))
}

suspend fun sha3(request: JSONRPCRequest): JSONRPCResponse {
  if (request.params.size != 1) {
    return invalidParams.copy(id = request.id)
  }
  if (!(request.params[0] is String)) {
    return invalidParams.copy(id = request.id)
  }
  try {
    val input = Bytes.fromHexString(request.params[0] as String)
    return JSONRPCResponse(id = request.id, result = Hash.hash(input).toHexString())
  } catch (e: IllegalArgumentException) {
    return invalidParams.copy(id = request.id)
  }
}
