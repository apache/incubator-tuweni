/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.rpc

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.bytes.Bytes
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Byte
import kotlin.Int
import kotlin.String
import kotlin.Throws
import kotlin.require

/**
 * Encoder responsible for encoding requests.
 *
 *
 * This encoder is stateful as it maintains a counter to provide different request ids over time.
 */
object RPCCodec {
  val counter = AtomicInteger(1)
  private val mapper = ObjectMapper()
  private fun nextRequestNumber(): Int {
    val requestNumber = counter.getAndIncrement()
    if (requestNumber < 1) {
      counter.set(1)
      return 1
    }
    return requestNumber
  }

  /**
   * Encode a message as a RPC request.
   *
   * @param body the body to encode as a RPC request
   * @param flags the flags of the RPC request
   * @return the message encoded as a RPC request
   */
  fun encodeRequest(body: String, vararg flags: RPCFlag): Bytes {
    return encodeRequest(Bytes.wrap(body.toByteArray(StandardCharsets.UTF_8)), nextRequestNumber(), *flags)
  }

  /**
   * Encode a message as a RPC request.
   *
   * @param body the body to encode as a RPC request
   * @param flags the flags of the RPC request
   * @return the message encoded as a RPC request
   */
  fun encodeRequest(body: Bytes, vararg flags: RPCFlag): Bytes {
    return encodeRequest(body, nextRequestNumber(), *flags)
  }

  /**
   * Encode a message as a RPC request.
   *
   * @param body the body to encode as a RPC request
   * @param requestNumber the number of the request. Must be equal or greater than one.
   * @param flags the flags of the RPC request
   * @return the message encoded as a RPC request
   */
  fun encodeRequest(body: Bytes, requestNumber: Int, vararg flags: RPCFlag): Bytes {
    require(requestNumber >= 1) { "Invalid request number" }
    var encodedFlags: Byte = 0
    for (flag in flags) {
      encodedFlags = flag.apply(encodedFlags)
    }
    return Bytes
      .concatenate(
        Bytes.of(encodedFlags),
        Bytes.ofUnsignedInt(body.size().toLong()),
        Bytes.ofUnsignedInt(requestNumber.toLong()),
        body
      )
  }

  /**
   * Encode a message as an RPC request.
   *
   * @param body the body to encode as an RPC request
   * @param requestNumber the request number
   * @param flags the flags of the RPC request (already encoded.)
   * @return the message encoded as an RPC request
   */
  fun encodeRequest(body: Bytes, requestNumber: Int, flags: Byte): Bytes {
    return Bytes
      .concatenate(
        Bytes.of(flags),
        Bytes.ofUnsignedInt(body.size().toLong()),
        Bytes.ofUnsignedInt(requestNumber.toLong()),
        body
      )
  }

  /**
   * Encode a message as a response to a RPC request.
   *
   * @param body the body to encode as the body of the response
   * @param requestNumber the request of the number. Must be equal or greater than one.
   * @param flagByte the flags of the RPC response encoded as a byte
   * @return the response encoded as a RPC response
   */
  fun encodeResponse(body: Bytes, requestNumber: Int, flagByte: Byte): Bytes {
    require(requestNumber >= 1) { "Invalid request number" }
    return Bytes
      .concatenate(
        Bytes.of(flagByte),
        Bytes.ofUnsignedInt(body.size().toLong()),
        Bytes.wrap(ByteBuffer.allocate(4).putInt(-requestNumber).array()),
        body
      )
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
  fun encodeResponse(body: Bytes, requestNumber: Int, flagByte: Byte, vararg flags: RPCFlag): Bytes {
    var mutableFlagByte = flagByte
    for (flag in flags) {
      mutableFlagByte = flag.apply(mutableFlagByte)
    }
    return encodeResponse(body, requestNumber, mutableFlagByte)
  }

  /**
   * Encodes a message with the body and headers set in the appropriate way to end a stream.
   *
   * @param requestNumber the request number
   * @return the response encoded as an RPC request
   * @throws JsonProcessingException if the encoding fails
   */
  @Throws(JsonProcessingException::class)
  fun encodeStreamEndRequest(requestNumber: Int): Bytes {
    val bool = true
    val bytes = mapper.writeValueAsBytes(bool)
    return encodeRequest(
      Bytes.wrap(bytes),
      requestNumber,
      RPCFlag.EndOrError.END,
      RPCFlag.BodyType.JSON,
      RPCFlag.Stream.STREAM
    )
  }

  /**
   * Encode a message as a response to a RPC request.
   *
   * @param body the body to encode as the body of the response
   * @param requestNumber the request of the number. Must be equal or greater than one.
   * @param flags the flags of the RPC request
   * @return the response encoded as a RPC response
   */
  fun encodeResponse(body: Bytes, requestNumber: Int, vararg flags: RPCFlag): Bytes {
    return encodeResponse(body, requestNumber, 0.toByte(), *flags)
  }
}
