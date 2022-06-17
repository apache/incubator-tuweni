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

  override fun equals(other: Any?) = other is JSONRPCRequest && this.method == other.method && params.contentEquals(other.params)
  override fun hashCode() = 31 * method.hashCode() + params.contentHashCode()

  fun serializeRequest(): String {
    return this.method + "|" + this.params.joinToString(",")
  }
}
