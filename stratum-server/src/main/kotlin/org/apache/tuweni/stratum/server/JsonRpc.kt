// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.stratum.server

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32

@JsonIgnoreProperties(ignoreUnknown = true)
data class JsonRpcRequest(
  @JsonProperty("jsonrpc") val jsonrpc: String?,
  @JsonProperty("method") val method: String,
  @JsonProperty("params") val params: MutableList<Any> = mutableListOf(),
  @JsonProperty("id") val id: Long
) {

  fun bytes32(i: Int): Bytes32 = Bytes32.fromHexStringLenient(params[i] as String)

  fun bytes(i: Int): Bytes = Bytes.fromHexStringLenient(params[i] as String)
}

data class JsonRpcSuccessResponse(
  @JsonProperty("id") val id: Long,
  @JsonProperty("jsonrpc") val jsonrpc: String = "2.0",
  @JsonProperty("result") val result: Any
)

data class JsonRpcSuccessResponseWithoutID(
  @JsonProperty("jsonrpc") val jsonrpc: String = "2.0",
  @JsonProperty("result") val result: Any
)
