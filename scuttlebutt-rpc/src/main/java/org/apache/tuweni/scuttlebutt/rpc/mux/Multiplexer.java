/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.scuttlebutt.rpc.mux;

import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.scuttlebutt.rpc.RPCAsyncRequest;
import net.consensys.cava.scuttlebutt.rpc.RPCMessage;
import net.consensys.cava.scuttlebutt.rpc.RPCStreamRequest;
import net.consensys.cava.scuttlebutt.rpc.mux.exceptions.ConnectionClosedException;

import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Multiplexes asynchronous requests and streams across a connection to a node. Handles multiple active requests and
 * streams across one connection.
 */
public interface Multiplexer {

  /**
   * Issue an 'async' type request to a node, which will eventually return a result from the node.
   *
   * @param request the request details
   *
   * @return an async result which will be completed with the result or an error if the request fails.
   */
  AsyncResult<RPCMessage> makeAsyncRequest(RPCAsyncRequest request);

  /**
   * Creates a request which opens a stream (e.g. a 'source' in the protocol docs.)
   *
   * @param request the request details
   * @param streamFactory a function which takes a 'Runnable' which closes the stream when ran, and returns a stream
   *        handler to pass messages to
   *
   * @throws JsonProcessingException
   */
  void openStream(RPCStreamRequest request, Function<Runnable, ScuttlebuttStreamHandler> streamFactory)
      throws JsonProcessingException,
      ConnectionClosedException;


  /**
   * Close the underlying connection
   */
  void close();

}
