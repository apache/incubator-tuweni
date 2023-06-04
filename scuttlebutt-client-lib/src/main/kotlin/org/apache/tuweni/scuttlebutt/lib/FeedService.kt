// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.scuttlebutt.lib.model.FeedMessage
import org.apache.tuweni.scuttlebutt.lib.model.ScuttlebuttMessageContent
import org.apache.tuweni.scuttlebutt.lib.model.StreamHandler
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest
import org.apache.tuweni.scuttlebutt.rpc.mux.ConnectionClosedException
import org.apache.tuweni.scuttlebutt.rpc.mux.Multiplexer
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler
import java.io.IOException
import java.util.Arrays
import java.util.function.Function

/**
 * A service for operations that concern scuttlebutt feeds.
 *
 * Should be accessed via a ScuttlebuttClient instance.
 *
 * @param multiplexer the RPC request multiplexer to make requests with.
 */
class FeedService(private val multiplexer: Multiplexer) {
  companion object {
    private val objectMapper = ObjectMapper()
  }

  /**
   * Publishes a message to the instance's own scuttlebutt feed, assuming the client established the connection using
   * keys authorising it to perform this operation.
   *
   * @param content the message to publish to the feed
   * @param <T> the content published should extend ScuttlebuttMessageContent to ensure the 'type' field is a String
   * @return the newly published message, asynchronously
   *
   * @throws JsonProcessingException if 'content' could not be marshalled to JSON.
   </T> */
  @Throws(JsonProcessingException::class)
  suspend fun <T : ScuttlebuttMessageContent?> publish(content: T): FeedMessage {
    val jsonNode = objectMapper.valueToTree<JsonNode>(content)
    val asyncRequest = RPCAsyncRequest(RPCFunction("publish"), Arrays.asList<Any>(jsonNode))
    val response = multiplexer.makeAsyncRequest(asyncRequest)
    return response.asJSON(
      objectMapper,
      FeedMessage::class.java,
    )
  }

  /**
   * Streams every message in the instance's database.
   *
   * @param streamHandler a function that can be used to construct the handler for processing the streamed messages,
   * using a runnable which can be ran to close the stream early.
   * @throws JsonProcessingException if the request to open the stream could not be made due to a JSON marshalling
   * error.
   *
   * @throws ConnectionClosedException if the stream could not be started because the connection is no longer open.
   */
  @Throws(JsonProcessingException::class, ConnectionClosedException::class)
  fun createFeedStream(streamHandler: Function<Runnable, StreamHandler<FeedMessage>>) {
    val streamRequest = RPCStreamRequest(RPCFunction("createFeedStream"), Arrays.asList())
    multiplexer.openStream(
      streamRequest,
    ) { closer: Runnable ->
      object : ScuttlebuttStreamHandler {
        var handler =
          streamHandler.apply(closer)

        override fun onMessage(message: RPCResponse) {
          try {
            handler.onMessage(
              message.asJSON(
                objectMapper,
                FeedMessage::class.java,
              ),
            )
          } catch (e: IOException) {
            handler.onStreamError(e)
          }
        }

        override fun onStreamEnd() {
          handler.onStreamEnd()
        }

        override fun onStreamError(ex: Exception) {
          handler.onStreamError(ex)
        }
      }
    }
  }
}
