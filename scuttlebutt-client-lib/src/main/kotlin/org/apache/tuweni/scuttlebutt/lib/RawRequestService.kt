// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib

import com.fasterxml.jackson.core.JsonProcessingException
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest
import org.apache.tuweni.scuttlebutt.rpc.mux.ConnectionClosedException
import org.apache.tuweni.scuttlebutt.rpc.mux.Multiplexer
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler
import java.util.function.Function

/**
 * Intended to make RPC requests which aren't supported by the higher level services possible.
 *
 * We cannot support every desired RPC request with higher level abstractions because it's possible to define custom
 * plugins with custom endpoints.
 */
class RawRequestService(private val multiplexer: Multiplexer) {
  @Throws(JsonProcessingException::class)
  suspend fun makeAsyncRequest(request: RPCAsyncRequest): RPCResponse {
    return multiplexer.makeAsyncRequest(request)
  }

  @Throws(JsonProcessingException::class, ConnectionClosedException::class)
  fun openStream(request: RPCStreamRequest, streamFactory: Function<Runnable, ScuttlebuttStreamHandler>) {
    multiplexer.openStream(request, streamFactory)
  }
}
