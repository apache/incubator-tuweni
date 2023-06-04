// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.rpc

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec.encodeRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCCodec.encodeResponse
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

internal class RPCEncodingTest {
  @Test
  fun rpcRoundtrip() {
    val message = encodeRequest(Bytes.fromHexString("deadbeef"), 3, RPCFlag.BodyType.BINARY, RPCFlag.Stream.STREAM)
    val decoded = RPCMessage(message)
    Assertions.assertTrue(decoded.stream())
    Assertions.assertEquals(Bytes.fromHexString("deadbeef"), decoded.body())
    Assertions.assertEquals(RPCFlag.BodyType.BINARY, decoded.bodyType())
    Assertions.assertEquals(3, decoded.requestNumber())
  }

  @Test
  @Throws(Exception::class)
  fun rpcRoundtripJSON() {
    val message = encodeRequest(
      Bytes.wrap("\"some JSON string\"".toByteArray(StandardCharsets.UTF_8)),
      RPCFlag.BodyType.JSON,
      RPCFlag.Stream.STREAM
    )
    val decoded = RPCMessage(message)
    Assertions.assertTrue(decoded.stream())
    Assertions.assertEquals(
      "some JSON string",
      decoded.asJSON(
        ObjectMapper(),
        String::class.java
      )
    )
    Assertions.assertEquals(RPCFlag.BodyType.JSON, decoded.bodyType())
    Assertions.assertEquals(RPCCodec.counter.get() - 1, decoded.requestNumber())
  }

  @Test
  fun rpcRoundtripUTF8String() {
    val message = encodeRequest("some message \\u02", RPCFlag.BodyType.UTF_8_STRING)
    val decoded = RPCMessage(message)
    Assertions.assertFalse(decoded.stream())
    Assertions.assertEquals("some message \\u02", decoded.asString())
    Assertions.assertEquals(RPCFlag.BodyType.UTF_8_STRING, decoded.bodyType())
    Assertions.assertEquals(RPCCodec.counter.get() - 1, decoded.requestNumber())
  }

  @Test
  fun rpcInvalidRequestNumber() {
    Assertions.assertThrows(
      IllegalArgumentException::class.java
    ) {
      encodeRequest(
        Bytes.fromHexString(
          "deadbeef"
        ),
        -1,
        RPCFlag.BodyType.BINARY
      )
    }
  }

  @Test
  fun rpcInvalidRequestNumberZero() {
    Assertions.assertThrows(
      IllegalArgumentException::class.java
    ) {
      encodeRequest(
        Bytes.fromHexString(
          "deadbeef"
        ),
        0,
        RPCFlag.BodyType.BINARY
      )
    }
  }

  @Test
  fun response() {
    val response = encodeResponse(Bytes.wrap("deadbeef".toByteArray(StandardCharsets.UTF_8)), 3, 1.toByte())
    val message = RPCMessage(response)
    Assertions.assertEquals(-3, message.requestNumber())
    Assertions.assertEquals("deadbeef", message.asString())
    Assertions.assertEquals(RPCFlag.BodyType.UTF_8_STRING, message.bodyType())
  }
}
