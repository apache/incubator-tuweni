// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.jsonrpc.methods

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.JSONRPCResponse

fun registerNet(
  networkId: String,
  listeningToConnections: Boolean = true,
  peerCount: () -> Int,
): Map<String, suspend (JSONRPCRequest) -> JSONRPCResponse> {
  val version = ConstantStringResult(networkId)
  val listening = ConstantBooleanResult(listeningToConnections)
  return mapOf(
    Pair("net_version", version::handle),
    Pair("net_listening", listening::handle),
    Pair(
      "net_peerCount",
      FunctionCallResult {
        Bytes.ofUnsignedInt(peerCount().toLong()).toQuantityHexString()
      }::handle,
    ),
  )
}
