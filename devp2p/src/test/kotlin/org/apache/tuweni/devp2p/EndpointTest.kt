// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p

import org.apache.tuweni.rlp.RLP
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.InetAddress

internal class EndpointTest {

  @Test
  fun shouldHaveExpectedMinimumSize() {
    val endpoint1 = Endpoint("127.0.0.1", 65535, 65535)
    val bytes1 = RLP.encode { r -> endpoint1.writeTo(r) }
    assertEquals(endpoint1.rlpSize(), bytes1.size())

    val endpoint2 = Endpoint("2001:4860:4860::8888", 65535, 65535)
    val bytes2 = RLP.encode { r -> endpoint2.writeTo(r) }
    assertEquals(endpoint2.rlpSize(), bytes2.size())
  }

  @Test
  fun endpointsWithSameHostAndPortsAreEqual() {
    val endpoint1 = Endpoint("127.0.0.1", 7654, 8765)
    val endpoint2 = Endpoint("127.0.0.1", 7654, 8765)
    assertEquals(endpoint1, endpoint2)

    val endpoint3 = Endpoint("127.0.0.1", 7654, null)
    val endpoint4 = Endpoint("127.0.0.1", 7654, null)
    assertEquals(endpoint3, endpoint4)
  }

  @Test
  fun endpointsWithDifferentHostsAreNotEqual() {
    val endpoint1 = Endpoint("127.0.0.1", 7654, 8765)
    val endpoint2 = Endpoint("127.0.0.2", 7654, 8765)
    assertNotEquals(endpoint1, endpoint2)
  }

  @Test
  fun endpointsWithDifferentUDPPortsAreNotEqual() {
    val endpoint1 = Endpoint("127.0.0.1", 7654, 8765)
    val endpoint2 = Endpoint("127.0.0.1", 7655, 8765)
    assertNotEquals(endpoint1, endpoint2)
  }

  @Test
  fun endpointsWithDifferentTCPPortsAreNotEqual() {
    val endpoint1 = Endpoint("127.0.0.1", 7654, 8765)
    val endpoint2 = Endpoint("127.0.0.1", 7654, 8766)
    assertNotEquals(endpoint1, endpoint2)

    val endpoint3 = Endpoint("127.0.0.1", 7654, null)
    assertNotEquals(endpoint1, endpoint3)
  }

  @Test
  fun invalidUDPPortThrowsIllegalArgument() {
    assertThrows<IllegalArgumentException> { Endpoint("127.0.0.1", 76543321, 8765) }
    assertThrows<IllegalArgumentException> { Endpoint("127.0.0.1", 0, 8765) }
  }

  @Test
  fun invalidTCPPortThrowsIllegalArgument() {
    assertThrows<IllegalArgumentException> { Endpoint("127.0.0.1", 7654, 87654321) }
    assertThrows<IllegalArgumentException> { Endpoint("127.0.0.1", 7654, 0) }
  }

  @Test
  fun shouldEncodeThenDecode() {
    val endpoint1 = Endpoint("127.0.0.1", 7654, 8765)
    val encoding1 = RLP.encode { writer -> endpoint1.writeTo(writer) }

    val endpoint2: Endpoint = RLP.decode(encoding1) { reader -> Endpoint.readFrom(reader) }
    assertEquals(endpoint1, endpoint2)

    val endpoint3 = Endpoint("127.0.0.1", 7654, null)
    val encoding2 = RLP.encode { writer -> endpoint3.writeTo(writer) }

    val endpoint4: Endpoint = RLP.decode(encoding2) { reader -> Endpoint.readFrom(reader) }
    assertEquals(endpoint3, endpoint4)
  }

  @Test
  fun shouldChangePortZeroToDefaultPort() {
    val encoding1 = RLP.encode { writer ->
      writer.writeByteArray(InetAddress.getByName("127.0.0.1").address)
      writer.writeInt(0)
      writer.writeInt(0)
    }

    val endpoint: Endpoint = RLP.decode(encoding1) { reader -> Endpoint.readFrom(reader) }
    assertEquals(30303, endpoint.udpPort)
  }
}
