// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JSONRPCResponse(
  @JsonProperty("id") val id: StringOrLong,
  @JsonProperty("result") val result: Any? = null,
  @JsonProperty(
    "error"
  ) val error: JSONRPCError? = null,
  @JsonProperty("jsonrpc") val jsonrpc: String = "2.0"
)

data class JSONRPCError(@JsonProperty("code") val code: Long, @JsonProperty("message") val message: String)

val parseError = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32700, "Parse error"))
val invalidRequest = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32600, "Invalid Request"))
val methodNotFound = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32601, "Method not found"))
val invalidParams = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32602, "Invalid params"))
val internalError = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32603, "Internal error"))
val tooManyRequests = JSONRPCResponse(
  id = StringOrLong(0),
  error = JSONRPCError(code = -32000, message = "Too many requests")
)
val methodNotEnabled = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32604, "Method not enabled"))
