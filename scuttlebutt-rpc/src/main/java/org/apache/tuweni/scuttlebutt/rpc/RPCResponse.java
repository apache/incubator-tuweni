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
import org.apache.tuweni.scuttlebutt.rpc.RPCFlag.BodyType;

import java.io.IOException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A successful RPC response.
 */
public class RPCResponse {

  private final Bytes body;
  private final BodyType bodyType;

  /**
   * A successful RPC response.
   *
   * @param body the body of the response in bytes
   * @param bodyType the type of the response (e.g. JSON, UTF-8 or binary.)
   */
  public RPCResponse(Bytes body, BodyType bodyType) {

    this.body = body;
    this.bodyType = bodyType;
  }

  /**
   * @return the RPC response body
   */
  public Bytes body() {
    return body;
  }

  /**
   * @return The type of the data contained in the body.
   */
  public BodyType bodyType() {
    return bodyType;
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

  public <T> T asJSON(ObjectMapper objectMapper, TypeReference<T> typeReference) throws IOException {
    return objectMapper.readerFor(typeReference).readValue(body().toArrayUnsafe());
  }


}
