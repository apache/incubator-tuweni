// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
