// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
      "{\"id\":\"foo\",\"info\":{\"api\":\"No\",\"canUpdateHistory\":true," +
        "\"client\":\"Apache Tuweni Ethstats\",\"name\":\"foo\",\"net\":\"eth\",\"node\":\"node\"," +
        "\"os\":\"os\",\"os_v\":\"123\",\"port\":123,\"protocol\":\"protocol\"},\"secret\":\"secret\"}",
      mapper.writeValueAsString(message),
    )
  }
}
