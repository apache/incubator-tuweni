// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.crawler

data class ClientIdInfo(val clientId: String) : Comparable<String> {

  val name: String
  val label: String
  val version: String
  val os: String
  val compiler: String
  init {
    val segments = clientId.split("/")
    if (segments.size == 4) {
      name = segments[0]
      label = ""
      version = segments[1]
      os = segments[2]
      compiler = segments[3]
    } else if (segments.size == 5) {
      name = segments[0]
      label = segments[1]
      version = segments[2]
      os = segments[3]
      compiler = segments[4]
    } else {
      name = segments[0]
      version = ""
      label = ""
      os = ""
      compiler = ""
    }
  }

  fun name() = name

  fun version() = version

  override fun compareTo(other: String): Int {
    return version().compareTo(other)
  }
}
