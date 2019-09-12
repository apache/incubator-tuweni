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

import org.apache.tuweni.bytes.Bytes;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Encoder responsible for encoding requests.
 * <p>
 * This encoder is stateful as it maintains a counter to provide different request ids over time.
 */
public final class RPCCodec {

  static final AtomicInteger counter = new AtomicInteger(1);

  private static ObjectMapper mapper = new ObjectMapper();

  private static int nextRequestNumber() {
    int requestNumber = counter.getAndIncrement();
    if (requestNumber < 1) {
      counter.set(1);
      return 1;
    }
    return requestNumber;
  }

  /**
   * Encode a message as a RPC request.
   * 
   * @param body the body to encode as a RPC request
   * @param flags the flags of the RPC request
   * @return the message encoded as a RPC request
   */
  public static Bytes encodeRequest(String body, RPCFlag... flags) {
    return encodeRequest(Bytes.wrap(body.getBytes(StandardCharsets.UTF_8)), nextRequestNumber(), flags);
  }

  /**
   * Encode a message as a RPC request.
   * 
   * @param body the body to encode as a RPC request
   * @param flags the flags of the RPC request
   * @return the message encoded as a RPC request
   */
  public static Bytes encodeRequest(Bytes body, RPCFlag... flags) {
    return encodeRequest(body, nextRequestNumber(), flags);
  }

  /**
   * Encode a message as a RPC request.
   * 
   * @param body the body to encode as a RPC request
   * @param requestNumber the number of the request. Must be equal or greater than one.
   * @param flags the flags of the RPC request
   * @return the message encoded as a RPC request
   */
  public static Bytes encodeRequest(Bytes body, int requestNumber, RPCFlag... flags) {
    if (requestNumber < 1) {
      throw new IllegalArgumentException("Invalid request number");
    }
    byte encodedFlags = 0;
    for (RPCFlag flag : flags) {
      encodedFlags = flag.apply(encodedFlags);
    }
    return Bytes.concatenate(
        Bytes.of(encodedFlags),
        Bytes.ofUnsignedInt(body.size()),
        Bytes.ofUnsignedInt(requestNumber),
        body);
  }

  /**
   * Encode a message as an RPC request.
   *
   * @param body the body to encode as an RPC request
   * @param requestNumber the request number
   * @param flags the flags of the RPC request (already encoded.)
   * @return the message encoded as an RPC request
   */
  public static Bytes encodeRequest(Bytes body, int requestNumber, byte flags) {
    return Bytes
        .concatenate(Bytes.of(flags), Bytes.ofUnsignedInt(body.size()), Bytes.ofUnsignedInt(requestNumber), body);
  }

  /**
   * Encode a message as a response to a RPC request.
   * 
   * @param body the body to encode as the body of the response
   * @param requestNumber the request of the number. Must be equal or greater than one.
   * @param flagByte the flags of the RPC response encoded as a byte
   * @return the response encoded as a RPC response
   */
  public static Bytes encodeResponse(Bytes body, int requestNumber, byte flagByte) {
    if (requestNumber < 1) {
      throw new IllegalArgumentException("Invalid request number");
    }
    return Bytes.concatenate(
        Bytes.of(flagByte),
        Bytes.ofUnsignedInt(body.size()),
        Bytes.wrap(ByteBuffer.allocate(4).putInt(-requestNumber).array()),
        body);
  }

  /**
   * Encode a message as a response to a RPC request.
   * 
   * @param body the body to encode as the body of the response
   * @param requestNumber the request of the number. Must be equal or greater than one.
   * @param flagByte the flags of the RPC response encoded as a byte
   * @param flags the flags of the RPC request
   * @return the response encoded as a RPC response
   */
  public static Bytes encodeResponse(Bytes body, int requestNumber, byte flagByte, RPCFlag... flags) {
    for (RPCFlag flag : flags) {
      flagByte = flag.apply(flagByte);
    }
    return encodeResponse(body, requestNumber, flagByte);
  }

  /**
   * Encodes a message with the body and headers set in the appropriate way to end a stream.
   *
   * @return the response encoded as an RPC request
   * @throws JsonProcessingException
   */
  public static Bytes encodeStreamEndRequest(int requestNumber) throws JsonProcessingException {
    Boolean bool = Boolean.TRUE;
    byte[] bytes = mapper.writeValueAsBytes(bool);
    return encodeRequest(
        Bytes.wrap(bytes),
        requestNumber,
        RPCFlag.EndOrError.END,
        RPCFlag.BodyType.JSON,
        RPCFlag.Stream.STREAM);
  }

  /**
   * Encode a message as a response to a RPC request.
   * 
   * @param body the body to encode as the body of the response
   * @param requestNumber the request of the number. Must be equal or greater than one.
   * @param flags the flags of the RPC request
   * @return the response encoded as a RPC response
   */
  public static Bytes encodeResponse(Bytes body, int requestNumber, RPCFlag... flags) {
    return encodeResponse(body, requestNumber, (byte) 0, flags);
  }
}
