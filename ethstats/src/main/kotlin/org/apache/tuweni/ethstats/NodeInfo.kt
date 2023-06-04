// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethstats

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonPropertyOrder(alphabetic = true)
data class NodeInfo @JsonCreator constructor(
  @JsonProperty("name")
  val name: String,
  @JsonProperty("node")
  val node: String,
  @JsonProperty("port")
  val port: Int,
  @JsonProperty("net")
  val net: String,
  @JsonProperty("protocol")
  val protocol: String,
  @JsonProperty("api")
  val api: String = "No",
  @JsonProperty("os")
  val os: String,
  @JsonProperty("os_v")
  private val osVer: String,
  @JsonProperty("canUpdateHistory")
  val canUpdateHistory: Boolean = true,
  @JsonProperty("client")
  val client: String = "Apache Tuweni Ethstats",
) {

  @JsonGetter("os_v")
  fun osVersion() = osVer
}
