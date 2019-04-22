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
package org.apache.tuweni.scuttlebutt.rpc.mux;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.scuttlebutt.handshake.vertx.ClientHandler;
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest;
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec;
import org.apache.tuweni.scuttlebutt.rpc.RPCFlag;
import org.apache.tuweni.scuttlebutt.rpc.RPCMessage;
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse;
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest;
import org.apache.tuweni.scuttlebutt.rpc.mux.exceptions.ConnectionClosedException;
import org.apache.tuweni.scuttlebutt.rpc.mux.exceptions.RPCRequestFailedException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.logl.Logger;
import org.logl.LoggerProvider;

/**
 * Handles RPC requests and responses from an active connection to a scuttlebutt node.
 */
public class RPCHandler implements Multiplexer, ClientHandler {

  private final Consumer<Bytes> messageSender;
  private final Logger logger;
  private final Runnable connectionCloser;
  private final ObjectMapper objectMapper;

  /**
   * We run each each update on the vertx event loop to update the request state synchronously, and to handle the
   * underlying connection closing by failing the in progress requests and not accepting future requests
   */
  private final Vertx vertx;

  private Map<Integer, CompletableAsyncResult<RPCResponse>> awaitingAsyncResponse = new HashMap<>();
  private Map<Integer, ScuttlebuttStreamHandler> streams = new HashMap<>();

  private boolean closed;

  /**
   * Makes RPC requests over a connection
   *
   * @param vertx The vertx instance to queue requests with
   * @param messageSender sends the request to the node
   * @param terminationFn closes the connection
   * @param objectMapper the objectMapper to serialize and deserialize message request and response bodies
   * @param logger
   */
  public RPCHandler(
      Vertx vertx,
      Consumer<Bytes> messageSender,
      Runnable terminationFn,
      ObjectMapper objectMapper,
      LoggerProvider logger) {
    this.vertx = vertx;
    this.messageSender = messageSender;
    this.connectionCloser = terminationFn;
    this.closed = false;
    this.objectMapper = objectMapper;

    this.logger = logger.getLogger("rpc handler");
  }

  @Override
  public AsyncResult<RPCResponse> makeAsyncRequest(RPCAsyncRequest request) throws JsonProcessingException {

    Bytes bodyBytes = request.toEncodedRpcMessage(objectMapper);

    CompletableAsyncResult<RPCResponse> result = AsyncResult.incomplete();

    Handler<Void> synchronizedAddRequest = (event) -> {
      if (closed) {
        result.completeExceptionally(new ConnectionClosedException());
      } else {
        RPCMessage message = new RPCMessage(bodyBytes);
        int requestNumber = message.requestNumber();

        awaitingAsyncResponse.put(requestNumber, result);
        Bytes bytes = RPCCodec.encodeRequest(message.body(), requestNumber, request.getRPCFlags());
        sendBytes(bytes);
      }
    };

    vertx.runOnContext(synchronizedAddRequest);
    return result;
  }

  @Override
  public void openStream(RPCStreamRequest request, Function<Runnable, ScuttlebuttStreamHandler> responseSink)
      throws JsonProcessingException {

    Bytes bodyBytes = request.toEncodedRpcMessage(objectMapper);

    Handler<Void> synchronizedRequest = (event) -> {

      RPCFlag[] rpcFlags = request.getRPCFlags();
      RPCMessage message = new RPCMessage(bodyBytes);
      int requestNumber = message.requestNumber();

      Bytes requestBytes = RPCCodec.encodeRequest(message.body(), requestNumber, rpcFlags);

      Runnable closeStreamHandler = () -> {

        try {
          Bytes streamEnd = RPCCodec.encodeStreamEndRequest(requestNumber);
          sendBytes(streamEnd);
        } catch (JsonProcessingException e) {
          logger.warn("Unexpectedly could not encode stream end message to JSON.");
        }

      };

      ScuttlebuttStreamHandler scuttlebuttStreamHandler = responseSink.apply(closeStreamHandler);

      if (closed) {
        scuttlebuttStreamHandler.onStreamError(new ConnectionClosedException());
      } else {
        streams.put(requestNumber, scuttlebuttStreamHandler);
        sendBytes(requestBytes);
      }


    };

    vertx.runOnContext(synchronizedRequest);
  }

  @Override
  public void close() {
    vertx.runOnContext((event) -> {
      connectionCloser.run();
    });
  }

  @Override
  public void receivedMessage(Bytes message) {

    Handler<Void> synchronizedHandleMessage = (event) -> {
      RPCMessage rpcMessage = new RPCMessage(message);

      // A negative request number indicates that this is a response, rather than a request that this node
      // should service
      if (rpcMessage.requestNumber() < 0) {
        handleResponse(rpcMessage);
      } else {
        handleRequest(rpcMessage);
      }
    };

    vertx.runOnContext(synchronizedHandleMessage);
  }

  @Override
  public void streamClosed() {

    Handler<Void> synchronizedCloseStream = (event) -> {
      closed = true;

      streams.forEach((key, streamHandler) -> {
        streamHandler.onStreamError(new ConnectionClosedException());
      });

      streams.clear();

      awaitingAsyncResponse.forEach((key, value) -> {
        if (!value.isDone()) {
          value.completeExceptionally(new ConnectionClosedException());
        }
      });

      awaitingAsyncResponse.clear();
    };

    vertx.runOnContext(synchronizedCloseStream);
  }

  private void handleRequest(RPCMessage rpcMessage) {
    // Not yet implemented
    logger.warn("Received incoming request, but we do not yet handle any requests: " + rpcMessage.asString());

  }

  private void handleResponse(RPCMessage response) {
    int requestNumber = response.requestNumber() * -1;

    if (logger.isDebugEnabled()) {
      logger.debug("Incoming response: " + response.asString());
    }

    byte rpcFlags = response.rpcFlags();

    boolean isStream = RPCFlag.Stream.STREAM.isApplied(rpcFlags);

    Optional<RPCRequestFailedException> exception = response.getException(objectMapper);

    if (isStream) {
      ScuttlebuttStreamHandler scuttlebuttStreamHandler = streams.get(requestNumber);

      if (scuttlebuttStreamHandler != null) {

        if (response.isSuccessfulLastMessage()) {
          streams.remove(requestNumber);
          scuttlebuttStreamHandler.onStreamEnd();
        } else if (exception.isPresent()) {
          scuttlebuttStreamHandler.onStreamError(exception.get());
        } else {
          RPCResponse successfulResponse = new RPCResponse(response.body(), response.bodyType());
          scuttlebuttStreamHandler.onMessage(successfulResponse);
        }
      } else {
        logger.warn(
            "Couldn't find stream handler for RPC response with request number "
                + requestNumber
                + " "
                + response.asString());
      }

    } else {

      CompletableAsyncResult<RPCResponse> rpcMessageFuture = awaitingAsyncResponse.remove(requestNumber);

      if (rpcMessageFuture != null) {

        if (exception.isPresent()) {
          rpcMessageFuture.completeExceptionally(exception.get());
        } else {
          RPCResponse successfulResponse = new RPCResponse(response.body(), response.bodyType());

          rpcMessageFuture.complete(successfulResponse);
        }

      } else {
        logger.warn(
            "Couldn't find async handler for RPC response with request number "
                + requestNumber
                + " "
                + response.asString());
      }
    }

  }

  private void sendBytes(Bytes bytes) {
    messageSender.accept(bytes);
  }

}
