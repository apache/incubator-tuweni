/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.lib;

import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.scuttlebutt.lib.model.CouldNotSerializeException;
import org.apache.tuweni.scuttlebutt.lib.model.FeedMessage;
import org.apache.tuweni.scuttlebutt.lib.model.ScuttlebuttMessageContent;
import org.apache.tuweni.scuttlebutt.lib.model.StreamHandler;
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest;
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction;
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse;
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest;
import org.apache.tuweni.scuttlebutt.rpc.mux.Multiplexer;
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler;
import org.apache.tuweni.scuttlebutt.rpc.mux.exceptions.ConnectionClosedException;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A service for operations that concern scuttlebutt feeds.
 *
 * Should be accessed via a ScuttlebuttClient instance.
 */
public final class FeedService {

  private final Multiplexer multiplexer;

  private final ObjectMapper objectMapper;

  /**
   *
   * @param multiplexer the RPC request multiplexer to make requests with.
   * @param mapper the object mapper to serialize published messages with.
   */
  protected FeedService(Multiplexer multiplexer, ObjectMapper mapper) {
    this.multiplexer = multiplexer;
    this.objectMapper = mapper;
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
   */
  public <T extends ScuttlebuttMessageContent> AsyncResult<FeedMessage> publish(T content)
      throws JsonProcessingException {

    JsonNode jsonNode = objectMapper.valueToTree(content);

    RPCAsyncRequest asyncRequest = new RPCAsyncRequest(new RPCFunction("publish"), Arrays.asList(jsonNode));

    return multiplexer.makeAsyncRequest(asyncRequest).thenApply(rpcResponse -> {
      try {
        return rpcResponse.asJSON(objectMapper, FeedMessage.class);
      } catch (IOException ex) {
        throw new CouldNotSerializeException(ex);
      }
    });

  }

  /**
   * Streams every message in the instance's database.
   *
   * @param streamHandler a function that can be used to construct the handler for processing the streamed messages,
   *        using a runnable which can be ran to close the stream early.
   * @throws JsonProcessingException if the request to open the stream could not be made due to a JSON marshalling
   *         error.
   *
   * @throws ConnectionClosedException if the stream could not be started because the connection is no longer open.
   */
  public void createFeedStream(Function<Runnable, StreamHandler<FeedMessage>> streamHandler)
      throws JsonProcessingException,
      ConnectionClosedException {

    RPCStreamRequest streamRequest = new RPCStreamRequest(new RPCFunction("createFeedStream"), Arrays.asList());

    multiplexer.openStream(streamRequest, (closer) -> new ScuttlebuttStreamHandler() {

      StreamHandler<FeedMessage> handler = streamHandler.apply(closer);

      @Override
      public void onMessage(RPCResponse message) {
        try {
          handler.onMessage(message.asJSON(objectMapper, FeedMessage.class));
        } catch (IOException e) {
          handler.onStreamError(e);
        }
      }

      @Override
      public void onStreamEnd() {
        handler.onStreamEnd();
      }

      @Override
      public void onStreamError(Exception ex) {
        handler.onStreamError(ex);
      }
    });

  }

}
