// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.rpc

/**
 * An RPC message response body which contains an error
 */
class RPCErrorBody {
  var name: String? = null
  var message: String? = null
  var stack: String? = null

  constructor() {}

  /**
   * A description of an error that occurred while performing an RPC request.
   *
   * @param name the name of the error type
   * @param message the message describing the error
   * @param stack the stack trace from the error
   */
  constructor(name: String?, message: String?, stack: String?) {
    this.name = name
    this.message = message
    this.stack = stack
  }
}
