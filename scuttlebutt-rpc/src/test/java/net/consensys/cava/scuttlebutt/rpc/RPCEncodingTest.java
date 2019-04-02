/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class RPCEncodingTest {

  @Test
  void rpcRoundtrip() {
    Bytes message =
        RPCCodec.encodeRequest(Bytes.fromHexString("deadbeef"), 3, RPCFlag.BodyType.BINARY, RPCFlag.Stream.STREAM);
    RPCMessage decoded = new RPCMessage(message);
    assertTrue(decoded.stream());
    assertEquals(Bytes.fromHexString("deadbeef"), decoded.body());
    assertEquals(RPCFlag.BodyType.BINARY, decoded.bodyType());
    assertEquals(3, decoded.requestNumber());
  }

  @Test
  void rpcRoundtripJSON() throws Exception {
    Bytes message = RPCCodec.encodeRequest(
        Bytes.wrap("\"some JSON string\"".getBytes(StandardCharsets.UTF_8)),
        RPCFlag.BodyType.JSON,
        RPCFlag.Stream.STREAM);
    RPCMessage decoded = new RPCMessage(message);
    assertTrue(decoded.stream());
    assertEquals("some JSON string", decoded.asJSON(String.class));
    assertEquals(RPCFlag.BodyType.JSON, decoded.bodyType());
    assertEquals(RPCCodec.counter.get() - 1, decoded.requestNumber());
  }

  @Test
  void rpcRoundtripUTF8String() {
    Bytes message = RPCCodec.encodeRequest("some message \\u02", RPCFlag.BodyType.UTF_8_STRING);
    RPCMessage decoded = new RPCMessage(message);
    assertFalse(decoded.stream());
    assertEquals("some message \\u02", decoded.asString());
    assertEquals(RPCFlag.BodyType.UTF_8_STRING, decoded.bodyType());
    assertEquals(RPCCodec.counter.get() - 1, decoded.requestNumber());
  }

  @Test
  void rpcInvalidRequestNumber() {
    assertThrows(
        IllegalArgumentException.class,
        () -> RPCCodec.encodeRequest(Bytes.fromHexString("deadbeef"), -1, RPCFlag.BodyType.BINARY));
  }

  @Test
  void rpcInvalidRequestNumberZero() {
    assertThrows(
        IllegalArgumentException.class,
        () -> RPCCodec.encodeRequest(Bytes.fromHexString("deadbeef"), 0, RPCFlag.BodyType.BINARY));
  }

  @Test
  void response() {
    Bytes response = RPCCodec.encodeResponse(Bytes.wrap("deadbeef".getBytes(StandardCharsets.UTF_8)), 3, (byte) 1);
    RPCMessage message = new RPCMessage(response);
    assertEquals(-3, message.requestNumber());
    assertEquals("deadbeef", message.asString());
    assertEquals(RPCFlag.BodyType.UTF_8_STRING, message.bodyType());
  }

}
