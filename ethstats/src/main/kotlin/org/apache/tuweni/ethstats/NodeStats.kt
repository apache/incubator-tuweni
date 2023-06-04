// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethstats

import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder(alphabetic = true)
data class NodeStats(
  val active: Boolean,
  val syncing: Boolean,
  val mining: Boolean,
  val hashrate: Int,
  val peers: Int,
  val gasPrice: Int,
  val uptime: Int,
)
