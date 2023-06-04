// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.rpc

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * The available type of Scuttlebutt RPC requests
 */
enum class RPCRequestType {
  /**
   * An 'async' request, which returns one result some time in the future.
   */
  @JsonProperty("async")
  ASYNC,

  /**
   * A 'source' type request, which begins a stream of results
   */
  @JsonProperty("source")
  SOURCE
}
