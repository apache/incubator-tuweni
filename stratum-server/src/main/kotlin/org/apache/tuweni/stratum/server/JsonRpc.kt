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
