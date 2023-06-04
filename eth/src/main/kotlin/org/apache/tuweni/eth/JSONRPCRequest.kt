// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

data class Id(val idAsString: String?, val idAsLong: Long?)

/**
 * JSONRPCRequest represents a JSON-RPC request to a JSON-RPC service.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class JSONRPCRequest constructor(
  @JsonProperty("id") val id: StringOrLong,
  @JsonProperty("method") val method: String,
  @JsonProperty("params") val params: Array<Any>,
  @JsonProperty("jsonrpc") val jsonrpc: String = "2.0"
) {

  companion object {
    /**
     * Deserialize a string into a JSON-RPC request.
     * The request is incomplete, as no id is set.
     *
     * The serialized form follows this formula:
     * <method>|<params, joined by comma>
     *
     * Example:
     * - eth_getBlockByNumber|latest,true
     */
    fun deserialize(serialized: String): JSONRPCRequest {
      val segments = serialized.split("|")
      return JSONRPCRequest(id = StringOrLong(0), method = segments[0], params = segments[1].split(",").toTypedArray())
    }
  }

  override fun equals(other: Any?) = other is JSONRPCRequest && this.method == other.method && params.contentEquals(
    other.params
  )
  override fun hashCode() = 31 * method.hashCode() + params.contentHashCode()

  fun serializeRequest(): String {
    return this.method + "|" + this.params.joinToString(",")
  }
}
