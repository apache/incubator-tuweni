// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.rpc

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec.encodeRequest

/**
 * A request which returns a 'source' type result (e.g. opens up a stream that is followed by the request ID.)
 *
 * @param function the function to be invoked
 * @param arguments the arguments for the function (can be any arbitrary class which can be marshalled into JSON.)
 */
class RPCStreamRequest(private val function: RPCFunction, private val arguments: List<Any>) {
  /**
   * Encodes message to bytes
   *
   * @param mapper the JSON mapper
   * @return The byte representation for the request after it is marshalled into a JSON string.
   * @throws JsonProcessingException if an error was thrown while marshalling to JSON
   */
  @Throws(JsonProcessingException::class)
  fun toEncodedRpcMessage(mapper: ObjectMapper?): Bytes {
    val body = RPCRequestBody(function.asList(), RPCRequestType.SOURCE, arguments)
    return encodeRequest(body.asBytes(mapper!!), *rPCFlags)
  }

  /**
   * The request flags
   *
   * @return The correct RPC flags for a stream request
   */
  val rPCFlags: Array<RPCFlag>
    get() = arrayOf(RPCFlag.Stream.STREAM, RPCFlag.BodyType.JSON)
}
