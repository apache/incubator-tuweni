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

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.concurrent.CompletableAsyncResult;
import net.consensys.cava.scuttlebutt.handshake.vertx.ClientHandler;
import net.consensys.cava.scuttlebutt.rpc.RPCAsyncRequest;
import net.consensys.cava.scuttlebutt.rpc.RPCCodec;
import net.consensys.cava.scuttlebutt.rpc.RPCErrorBody;
import net.consensys.cava.scuttlebutt.rpc.RPCFlag;
import net.consensys.cava.scuttlebutt.rpc.RPCMessage;
import net.consensys.cava.scuttlebutt.rpc.RPCStreamRequest;
import net.consensys.cava.scuttlebutt.rpc.mux.exceptions.ConnectionClosedException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import org.logl.Logger;
import org.logl.LoggerProvider;

/**
 * Handles RPC requests and responses from an active connection to a scuttlebutt node
 *
 * Note: the public methods on this class are synchronized so that a request is rejected if the connection has been
 * closed before it begins and any 'in flight' requests are ended exceptionally with a 'connection closed' error without
 * new incoming requests being added to the maps by threads.
 *
 * In the future,we could perhaps be carefully more fine grained about the locking if we require a high degree of
 * concurrency.
 *
 */
public class RPCHandler implements Multiplexer, ClientHandler {

  private final Consumer<Bytes> messageSender;
  private final Logger logger;
  private final Runnable connectionCloser;
  private final ObjectMapper objectMapper;

  private Map<Integer, CompletableAsyncResult<RPCMessage>> awaitingAsyncResponse = new HashMap<>();
  private Map<Integer, ScuttlebuttStreamHandler> streams = new HashMap<>();

  private boolean closed;

  /**
   * Makes RPC requests over a connection
   *
   * @param messageSender sends the request to the node
   * @param terminationFn closes the connection
   * @param objectMapper the objectMapper to serialize and deserialize message request and response bodies
   * @param logger
   */
  public RPCHandler(
      Consumer<Bytes> messageSender,
      Runnable terminationFn,
      ObjectMapper objectMapper,
      LoggerProvider logger) {
    this.messageSender = messageSender;
    this.connectionCloser = terminationFn;
    this.closed = false;
    this.objectMapper = objectMapper;

    this.logger = logger.getLogger("rpc handler");
  }

  @Override
  public synchronized AsyncResult<RPCMessage> makeAsyncRequest(RPCAsyncRequest request) {

    CompletableAsyncResult<RPCMessage> result = AsyncResult.incomplete();

    if (closed) {
      result.completeExceptionally(new ConnectionClosedException());
    }

    try {
      RPCMessage message = new RPCMessage(request.toEncodedRpcMessage(objectMapper));
      int requestNumber = message.requestNumber();
      awaitingAsyncResponse.put(requestNumber, result);
      Bytes bytes = RPCCodec.encodeRequest(message.body(), requestNumber, request.getRPCFlags());
      messageSender.accept(bytes);

    } catch (JsonProcessingException e) {
      result.completeExceptionally(e);
    }

    return result;
  }

  @Override
  public synchronized void openStream(
      RPCStreamRequest request,
      Function<Runnable, ScuttlebuttStreamHandler> responseSink) throws JsonProcessingException,
      ConnectionClosedException {

    if (closed) {
      throw new ConnectionClosedException();
    }

    try {
      RPCFlag[] rpcFlags = request.getRPCFlags();
      RPCMessage message = new RPCMessage(request.toEncodedRpcMessage(objectMapper));
      int requestNumber = message.requestNumber();

      Bytes bytes = RPCCodec.encodeRequest(message.body(), requestNumber, rpcFlags);
      messageSender.accept(bytes);

      Runnable closeStreamHandler = new Runnable() {
        @Override
        public void run() {

          try {
            Bytes bytes = RPCCodec.encodeStreamEndRequest(requestNumber);
            messageSender.accept(bytes);
          } catch (JsonProcessingException e) {
            logger.warn("Unexpectedly could not encode stream end message to JSON.");
          }

        }
      };

      ScuttlebuttStreamHandler scuttlebuttStreamHandler = responseSink.apply(closeStreamHandler);

      streams.put(requestNumber, scuttlebuttStreamHandler);
    } catch (JsonProcessingException ex) {
      throw ex;
    }
  }

  @Override
  public synchronized void close() {
    connectionCloser.run();
  }

  @Override
  public synchronized void receivedMessage(Bytes message) {

    RPCMessage rpcMessage = new RPCMessage(message);

    // A negative request number indicates that this is a response, rather than a request that this node
    // should service
    if (rpcMessage.requestNumber() < 0) {
      handleResponse(rpcMessage);
    } else {
      handleRequest(rpcMessage);
    }

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

    if (isStream) {
      ScuttlebuttStreamHandler scuttlebuttStreamHandler = streams.get(requestNumber);

      if (scuttlebuttStreamHandler != null) {

        if (response.isSuccessfulLastMessage()) {
          streams.remove(requestNumber);
          scuttlebuttStreamHandler.onStreamEnd();
        } else if (response.isErrorMessage()) {

          Optional<RPCErrorBody> errorBody = response.getErrorBody(objectMapper);

          if (errorBody.isPresent()) {
            scuttlebuttStreamHandler.onStreamError(new Exception(errorBody.get().getMessage()));
          } else {
            // This shouldn't happen, but for safety we fall back to just writing the whole body in the exception message
            // if we fail to marshall it for whatever reason
            scuttlebuttStreamHandler.onStreamError(new Exception(response.asString()));
          }

        } else {
          scuttlebuttStreamHandler.onMessage(response);
        }
      } else {
        logger.warn(
            "Couldn't find stream handler for RPC response with request number "
                + requestNumber
                + " "
                + response.asString());
      }

    } else {

      CompletableAsyncResult<RPCMessage> rpcMessageFuture = awaitingAsyncResponse.get(requestNumber);

      if (rpcMessageFuture != null) {
        rpcMessageFuture.complete(response);
        awaitingAsyncResponse.remove(requestNumber);
      } else {
        logger.warn(
            "Couldn't find async handler for RPC response with request number "
                + requestNumber
                + " "
                + response.asString());
      }
    }

  }

  @Override
  public void streamClosed() {
    this.closed = true;

    streams.forEach((key, streamHandler) -> {
      streamHandler.onStreamError(new ConnectionClosedException());
    });

    streams.clear();

    awaitingAsyncResponse.forEach((key, value) -> {
      if (!value.isDone()) {
        value.completeExceptionally(new ConnectionClosedException());
      }

    });


  }
}
