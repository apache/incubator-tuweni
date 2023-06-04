// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.rpc.mux

import com.fasterxml.jackson.core.JsonProcessingException
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest
import java.util.function.Function

/**
 * Multiplexes asynchronous requests and streams across a connection to a node. Handles multiple active requests and
 * streams across one connection.
 */
interface Multiplexer {
  /**
   * Issue an 'async' type request to a node, which will eventually return a result from the node.
   *
   * @param request the request details
   *
   * @return an async result which will be completed with the result or an error if the request fails.
   * @throws JsonProcessingException if JSON marshalling fails.
   */
  @Throws(JsonProcessingException::class)
  suspend fun makeAsyncRequest(request: RPCAsyncRequest): RPCResponse

  /**
   * Creates a request which opens a stream (e.g. a 'source' in the protocol docs.)
   *
   * @param request the request details
   * @param streamFactory a function which takes a 'Runnable' which closes the stream when ran, and returns a stream
   * handler to pass messages to
   *
   * @throws JsonProcessingException if JSON marshalling fails.
   */
  @Throws(JsonProcessingException::class)
  fun openStream(request: RPCStreamRequest, streamFactory: Function<Runnable, ScuttlebuttStreamHandler>)

  /**
   * Close the underlying connection
   */
  fun close()
}
