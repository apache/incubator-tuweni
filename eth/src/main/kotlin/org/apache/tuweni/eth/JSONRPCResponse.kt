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

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class JSONRPCResponse(@JsonProperty("id") val id: StringOrLong, @JsonProperty("result") val result: Any? = null, @JsonProperty("error") val error: JSONRPCError? = null, @JsonProperty("jsonrpc") val jsonrpc: String = "2.0")

data class JSONRPCError(@JsonProperty("code") val code: Long, @JsonProperty("message") val message: String)

val parseError = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32700, "Parse error"))
val invalidRequest = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32600, "Invalid Request"))
val methodNotFound = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32601, "Method not found"))
val invalidParams = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32602, "Invalid params"))
val internalError = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32603, "Internal error"))
val tooManyRequests = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(code = -32000, message = "Too many requests"))
val methodNotEnabled = JSONRPCResponse(id = StringOrLong(0), error = JSONRPCError(-32604, "Method not enabled"))
