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

/**
 * A scuttlebutt RPC function namespace and name representation.
 */
class RPCFunction {
  private val namespace: List<String>
  private val functionName: String

  /**
   *
   * @param namespace the namespace of the function (e.g. ['blobs']. May be empty if there is no namespace for the
   * function.
   * @param functionName the function (e.g. 'add'.)
   */
  constructor(namespace: List<String>, functionName: String) {
    this.namespace = namespace
    this.functionName = functionName
  }

  constructor(functionName: String) {
    namespace = ArrayList()
    this.functionName = functionName
  }

  /**
   * Provides the list representation of the namespace and function call.
   *
   * @return The list representation of the namespace and function call.
   */
  fun asList(): List<String> {
    val list: MutableList<String> = ArrayList()
    list.addAll(namespace)
    list.add(functionName)
    return list
  }
}
