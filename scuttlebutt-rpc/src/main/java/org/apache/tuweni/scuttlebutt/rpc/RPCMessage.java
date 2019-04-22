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
package org.apache.tuweni.scuttlebutt.rpc;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.scuttlebutt.rpc.mux.exceptions.RPCRequestFailedException;

import java.io.IOException;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Decoded RPC message, making elements of the message available directly.
 */
public final class RPCMessage {

  private final byte rpcFlags;
  private final boolean stream;
  private final boolean lastMessageOrError;
  private final RPCFlag.BodyType bodyType;
  private final Bytes body;
  private final int requestNumber;

  /**
   * Default constructor
   *
   * @param messageBytes the bytes of the encoded message.
   */
  public RPCMessage(Bytes messageBytes) {
    rpcFlags = messageBytes.get(0);
    stream = RPCFlag.Stream.STREAM.isApplied(rpcFlags);
    lastMessageOrError = RPCFlag.EndOrError.END.isApplied(rpcFlags);
    if (RPCFlag.BodyType.JSON.isApplied(rpcFlags)) {
      bodyType = RPCFlag.BodyType.JSON;
    } else if (RPCFlag.BodyType.UTF_8_STRING.isApplied(rpcFlags)) {
      bodyType = RPCFlag.BodyType.UTF_8_STRING;
    } else {
      bodyType = RPCFlag.BodyType.BINARY;
    }

    int bodySize = messageBytes.slice(1, 4).toInt();

    requestNumber = messageBytes.slice(5, 4).toInt();

    if (messageBytes.size() < bodySize + 9) {
      throw new IllegalArgumentException(
          "Message body " + (messageBytes.size() - 9) + " is less than body size " + bodySize);
    }

    body = messageBytes.slice(9, bodySize);
  }

  /**
   * Indicates if the message is part of a stream.
   *
   * @return true if the message if part of a stream
   */
  public boolean stream() {
    return stream;
  }

  /**
   * Indicates if the message is either the last in the stream or an error message.
   *
   * @return true if this message is the last one, or an error
   */
  public boolean lastMessageOrError() {
    return lastMessageOrError;
  }

  /**
   *
   * @return true if this is a last message in a stream, and it is not an error
   */
  public boolean isSuccessfulLastMessage() {
    return lastMessageOrError() && asString().equals("true");
  }

  /**
   *
   * @return true if this is an error message response
   */
  public boolean isErrorMessage() {
    return lastMessageOrError && !isSuccessfulLastMessage();
  }

  /**
   * @param objectMapper the object mapper to deserialize with
   * @return the RPC error response body, if this is an error response - nothing otherwise
   */
  public Optional<RPCErrorBody> getErrorBody(ObjectMapper objectMapper) {

    if (!isErrorMessage()) {
      // If the body of the response is 'true' or the error flag isn't set, it's a successful end condition
      return Optional.empty();
    } else {
      try {
        return Optional.of(asJSON(objectMapper, RPCErrorBody.class));
      } catch (IOException e) {
        return Optional.empty();
      }
    }
  }

  /**
   *
   * @param objectMapper the objectmatter to deserialize the error with.
   *
   * @return an exception if this represents an error RPC response, otherwise nothing
   */
  public Optional<RPCRequestFailedException> getException(ObjectMapper objectMapper) {
    if (isErrorMessage()) {
      Optional<RPCRequestFailedException> exception =
          getErrorBody(objectMapper).map(errorBody -> new RPCRequestFailedException(errorBody.getMessage()));

      if (!exception.isPresent()) {
        // If we failed to deserialize into the RPCErrorBody type there may be a bug in the server implementation
        // which prevented it returning the correct type, so we just print whatever it returned
        return Optional.of(new RPCRequestFailedException(this.asString()));
      } else {
        return exception;
      }

    } else {
      return Optional.empty();
    }
  }

  /**
   * Provides the type of the body of the message: a binary message, a UTF-8 string or a JSON message.
   * 
   * @return the type of the body: a binary message, a UTF-8 string or a JSON message
   */
  public RPCFlag.BodyType bodyType() {
    return bodyType;
  }

  /**
   * Provides the request number of the message.
   * 
   * @return the request number of the message
   */
  public int requestNumber() {
    return requestNumber;
  }

  /**
   * Provides the body of the message.
   * 
   * @return the bytes of the body of the message
   */
  public Bytes body() {
    return body;
  }

  /**
   * Provide the RPC flags set on the message.
   * 
   * @return the RPC flags set on the message as a single byte.
   */
  public byte rpcFlags() {
    return rpcFlags;
  }

  /**
   * Provides the body of the message as a UTF-8 string.
   * 
   * @return the body of the message as a UTF-8 string
   */
  public String asString() {
    return new String(body().toArrayUnsafe(), UTF_8);
  }

  /**
   * Provides the body of the message, marshalled as a JSON object.
   *
   * @param objectMapper the object mapper to deserialize with
   * @param clazz the JSON object class
   * @param <T> the matching JSON object class
   * @return a new instance of the JSON object class
   * @throws IOException if an error occurs during marshalling
   */
  public <T> T asJSON(ObjectMapper objectMapper, Class<T> clazz) throws IOException {
    return objectMapper.readerFor(clazz).readValue(body().toArrayUnsafe());
  }
}
