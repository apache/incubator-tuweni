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
