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

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.scuttlebutt.rpc.RPCFlag.BodyType
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCRequestFailedException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.Optional

/**
 * Decoded RPC message, making elements of the message available directly.
 *
 * @param messageBytes the bytes of the encoded message.
 */
class RPCMessage(messageBytes: Bytes) {
  private val rpcFlags: Byte
  private val stream: Boolean
  private val lastMessageOrError: Boolean
  private val bodyType: BodyType
  private val body: Bytes
  private val requestNumber: Int

  init {
    rpcFlags = messageBytes[0]
    stream = RPCFlag.Stream.STREAM.isApplied(rpcFlags)
    lastMessageOrError = RPCFlag.EndOrError.END.isApplied(rpcFlags)
    bodyType = if (BodyType.JSON.isApplied(rpcFlags)) {
      BodyType.JSON
    } else if (BodyType.UTF_8_STRING.isApplied(rpcFlags)) {
      BodyType.UTF_8_STRING
    } else {
      BodyType.BINARY
    }
    val bodySize = messageBytes.slice(1, 4).toInt()
    requestNumber = messageBytes.slice(5, 4).toInt()
    require(messageBytes.size() >= bodySize + 9) { "Message body " + (messageBytes.size() - 9) + " is less than body size " + bodySize }
    body = messageBytes.slice(9, bodySize)
  }

  /**
   * Indicates if the message is part of a stream.
   *
   * @return true if the message if part of a stream
   */
  fun stream(): Boolean {
    return stream
  }

  /**
   * Indicates if the message is either the last in the stream or an error message.
   *
   * @return true if this message is the last one, or an error
   */
  fun lastMessageOrError(): Boolean {
    return lastMessageOrError
  }

  /**
   * Returns true is this is the last message in the stream
   *
   * @return true if this is a last message in a stream, and it is not an error
   */
  val isSuccessfulLastMessage: Boolean
    get() = lastMessageOrError() && asString() == "true"

  /**
   * Returns true if this an error message.
   *
   * @return true if this is an error message response
   */
  val isErrorMessage: Boolean
    get() = lastMessageOrError && !isSuccessfulLastMessage

  /**
   * Gets error body
   *
   * @param objectMapper the object mapper to deserialize with
   * @return the RPC error response body, if this is an error response - nothing otherwise
   */
  fun getErrorBody(objectMapper: ObjectMapper): Optional<RPCErrorBody> {
    return if (!isErrorMessage) {
      // If the body of the response is 'true' or the error flag isn't set, it's a successful end condition
      Optional.empty()
    } else {
      try {
        Optional.of(asJSON(objectMapper, RPCErrorBody::class.java))
      } catch (e: IOException) {
        Optional.empty()
      }
    }
  }

  /**
   * Gets exception from object mapper.
   *
   * @param objectMapper the object mapper to deserialize the error with.
   *
   * @return an exception if this represents an error RPC response, otherwise nothing
   */
  fun getException(objectMapper: ObjectMapper): Optional<RPCRequestFailedException> {
    return if (isErrorMessage) {
      val exception = getErrorBody(objectMapper).map { errorBody: RPCErrorBody ->
        RPCRequestFailedException(
          errorBody.message!!
        )
      }
      if (!exception.isPresent) {
        // If we failed to deserialize into the RPCErrorBody type there may be a bug in the server implementation
        // which prevented it returning the correct type, so we just print whatever it returned
        Optional.of(RPCRequestFailedException(asString()))
      } else {
        exception
      }
    } else {
      Optional.empty()
    }
  }

  /**
   * Provides the type of the body of the message: a binary message, a UTF-8 string or a JSON message.
   *
   * @return the type of the body: a binary message, a UTF-8 string or a JSON message
   */
  fun bodyType(): BodyType {
    return bodyType
  }

  /**
   * Provides the request number of the message.
   *
   * @return the request number of the message
   */
  fun requestNumber(): Int {
    return requestNumber
  }

  /**
   * Provides the body of the message.
   *
   * @return the bytes of the body of the message
   */
  fun body(): Bytes {
    return body
  }

  /**
   * Provide the RPC flags set on the message.
   *
   * @return the RPC flags set on the message as a single byte.
   */
  fun rpcFlags(): Byte {
    return rpcFlags
  }

  /**
   * Provides the body of the message as a UTF-8 string.
   *
   * @return the body of the message as a UTF-8 string
   */
  fun asString(): String {
    return String(body().toArrayUnsafe(), StandardCharsets.UTF_8)
  }

  /**
   * Provides the body of the message, marshalled as a JSON object.
   *
   * @param objectMapper the object mapper to deserialize with
   * @param clazz the JSON object class
   * @param <T> the matching JSON object class
   * @return a new instance of the JSON object class
   * @throws IOException if an error occurs during marshalling
   </T> */
  @Throws(IOException::class)
  fun <T> asJSON(objectMapper: ObjectMapper, clazz: Class<T>?): T {
    return objectMapper.readerFor(clazz).readValue(body().toArrayUnsafe())
  }
}
