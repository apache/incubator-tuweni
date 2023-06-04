// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
