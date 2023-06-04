// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.rpc

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RPCFlagTest {
  @Test
  fun testBinary() {
    val zero: Byte = 0
    Assertions.assertFalse(RPCFlag.BodyType.JSON.isApplied(zero))
    Assertions.assertFalse(RPCFlag.BodyType.UTF_8_STRING.isApplied(zero))
    Assertions.assertTrue(RPCFlag.BodyType.BINARY.isApplied(zero))
  }

  @Test
  fun testJSON() {
    val json = RPCFlag.BodyType.JSON.apply(0.toByte())
    Assertions.assertFalse(RPCFlag.BodyType.BINARY.isApplied(json))
    Assertions.assertFalse(RPCFlag.BodyType.UTF_8_STRING.isApplied(json))
    Assertions.assertTrue(RPCFlag.BodyType.JSON.isApplied(json))
  }

  @Test
  fun testUTF8String() {
    val utf8String = RPCFlag.BodyType.UTF_8_STRING.apply(0.toByte())
    Assertions.assertFalse(RPCFlag.BodyType.BINARY.isApplied(utf8String))
    Assertions.assertFalse(RPCFlag.BodyType.JSON.isApplied(utf8String))
    Assertions.assertTrue(RPCFlag.BodyType.UTF_8_STRING.isApplied(utf8String))
  }

  @Test
  fun testBodyType() {
    val utf8String = RPCFlag.BodyType.UTF_8_STRING.apply(0.toByte())
    val json = RPCFlag.BodyType.JSON.apply(0.toByte())
    val zero: Byte = 0
    assertEquals(RPCFlag.BodyType.UTF_8_STRING, RPCFlag.BodyType.extractBodyType(utf8String))
    assertEquals(RPCFlag.BodyType.JSON, RPCFlag.BodyType.extractBodyType(json))
    assertEquals(RPCFlag.BodyType.BINARY, RPCFlag.BodyType.extractBodyType(zero))
  }
}
