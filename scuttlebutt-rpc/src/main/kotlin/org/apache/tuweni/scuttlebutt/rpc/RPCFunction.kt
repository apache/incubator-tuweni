// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
