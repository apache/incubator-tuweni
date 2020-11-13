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

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The request payload of an RPC request to another node. The fields are as specified in the scuttlebutt protocol docs
 */
public class RPCRequestBody {

  private final List<String> name;
  private final RPCRequestType type;
  private final List<Object> args;

  /**
   *
   * @param name the function to be in invoked. If the function is in a namespace, the first n-1 items in the array are
   *        the namespace followed by the function name (e.g. 'blobs.get' becomes ['blobs', 'get']). If the function is
   *        not in a namespace, it is an array with one item (e.g. ['createFeedStream'].
   * @param type the type of the request (e.g. stream or async.)
   * @param args The args passed to the function being invoked. Each item can be any arbitrary object which is JSON
   *        serializable (e.g. String, Int, list, object.)
   */
  public RPCRequestBody(List<String> name, RPCRequestType type, List<Object> args) {
    this.name = name;
    this.type = type;
    this.args = args;
  }

  public List<String> getName() {
    return name;
  }

  public RPCRequestType getType() {
    return type;
  }

  public List<Object> getArgs() {
    return args;
  }

  /**
   * Serialize body to bytes.
   *
   * @param objectMapper the object mapper to serialize to bytes with
   * @return the bytes representation of this RPC request body. The request is first encoded into JSON, then from JSON
   *         to a byte array
   * @throws JsonProcessingException thrown if there is a problem transforming the object to JSON.
   */
  public Bytes asBytes(ObjectMapper objectMapper) throws JsonProcessingException {
    byte[] bytes = objectMapper.writeValueAsBytes(this);
    return Bytes.wrap(bytes);
  }


}
