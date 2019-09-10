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
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest;
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse;
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest;
import org.apache.tuweni.scuttlebutt.rpc.mux.Multiplexer;
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler;
import org.apache.tuweni.scuttlebutt.rpc.mux.exceptions.ConnectionClosedException;

import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Intended to make RPC requests which aren't supported by the higher level services possible.
 *
 * We cannot support every desired RPC request with higher level abstractions because it's possible to define custom
 * plugins with custom endpoints.
 */
public final class RawRequestService {

  private final Multiplexer multiplexer;

  protected RawRequestService(Multiplexer multiplexer) {
    this.multiplexer = multiplexer;
  }

  public AsyncResult<RPCResponse> makeAsyncRequest(RPCAsyncRequest request) throws JsonProcessingException {
    return multiplexer.makeAsyncRequest(request);
  }

  public void openStream(RPCStreamRequest request, Function<Runnable, ScuttlebuttStreamHandler> streamFactory)
      throws JsonProcessingException,
      ConnectionClosedException {
    multiplexer.openStream(request, streamFactory);
  }


}
