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

import com.fasterxml.jackson.annotation.JsonProperty

data class JSONRPCResponse(@JsonProperty("id") val id: Int, @JsonProperty("result") val result: Any? = null, @JsonProperty("error") val error: Any? = null)

val parseError = JSONRPCResponse(id = 0, error = mapOf(Pair("code", -32700), Pair("message", "Parse error")))
val invalidRequest = JSONRPCResponse(id = 0, error = mapOf(Pair("code", -32600), Pair("message", "Invalid Request")))
val methodNotFound = JSONRPCResponse(id = 0, error = mapOf(Pair("code", -32601), Pair("message", "Method not found")))
val invalidParams = JSONRPCResponse(id = 0, error = mapOf(Pair("code", -32602), Pair("message", "Invalid params")))
val internalError = JSONRPCResponse(id = 0, error = mapOf(Pair("code", -32603), Pair("message", "Internal error")))
