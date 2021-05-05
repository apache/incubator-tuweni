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
package org.apache.tuweni.ethstats

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthMessageTest {

  @Test
  fun toJson() {
    val nodeInfo = NodeInfo("foo", "node", 123, "eth", "protocol", os = "os", osVer = "123")
    val message = AuthMessage(nodeInfo, "foo", "secret")
    val mapper = ObjectMapper()
    assertEquals(
      "{\"id\":\"foo\",\"info\":{\"api\":\"No\",\"canUpdateHistory\":true,\"client\":\"Apache Tuweni Ethstats\",\"name\":\"foo\",\"net\":\"eth\",\"node\":\"node\",\"os\":\"os\",\"os_v\":\"123\",\"port\":123,\"protocol\":\"protocol\"},\"secret\":\"secret\"}",
      mapper.writeValueAsString(message)
    )
  }
}
