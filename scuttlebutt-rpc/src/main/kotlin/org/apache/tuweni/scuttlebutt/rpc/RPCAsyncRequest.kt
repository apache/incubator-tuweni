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
package org.apache.tuweni.scuttlebutt.rpc

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec.encodeRequest

class RPCAsyncRequest
/**
 *
 * @param function the function to be in invoked. If the function is in a namespace, the first n-1 items in the array
 * are the namespace followed by the function name (e.g. 'blobs.get' becomes ['blobs', 'get']).
 * @param arguments The arguments passed to the function being invoked. Each item can be any arbitrary object which is
 * JSON serializable (e.g. String, Int, list, object.)
 */(private val function: RPCFunction, private val arguments: List<Any>) {
  /**
   * Encode the RPC request as bytes.
   *
   * @param objectMapper the object mapper to serialize the request with
   * @return an RPC request serialized into bytes
   * @throws JsonProcessingException thrown if there is an error while serializing the request to bytes
   */
  @Throws(JsonProcessingException::class)
  fun toEncodedRpcMessage(objectMapper: ObjectMapper): Bytes {
    return encodeRequest(
      RPCRequestBody(function.asList(), RPCRequestType.ASYNC, arguments).asBytes(objectMapper),
      *rPCFlags
    )
  }

  val rPCFlags: Array<RPCFlag>
    get() = arrayOf(RPCFlag.BodyType.JSON)
}
