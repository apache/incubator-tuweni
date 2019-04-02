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
package net.consensys.cava.scuttlebutt.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RPCFlagTest {

  @Test
  void testBinary() {
    byte zero = 0;
    assertFalse(RPCFlag.BodyType.JSON.isApplied(zero));
    assertFalse(RPCFlag.BodyType.UTF_8_STRING.isApplied(zero));
    assertTrue(RPCFlag.BodyType.BINARY.isApplied(zero));
  }

  @Test
  void testJSON() {
    byte json = RPCFlag.BodyType.JSON.apply((byte) 0);
    assertFalse(RPCFlag.BodyType.BINARY.isApplied(json));
    assertFalse(RPCFlag.BodyType.UTF_8_STRING.isApplied(json));
    assertTrue(RPCFlag.BodyType.JSON.isApplied(json));
  }

  @Test
  void testUTF8String() {
    byte utf8String = RPCFlag.BodyType.UTF_8_STRING.apply((byte) 0);
    assertFalse(RPCFlag.BodyType.BINARY.isApplied(utf8String));
    assertFalse(RPCFlag.BodyType.JSON.isApplied(utf8String));
    assertTrue(RPCFlag.BodyType.UTF_8_STRING.isApplied(utf8String));
  }

  @Test
  void testBodyType() {
    byte utf8String = RPCFlag.BodyType.UTF_8_STRING.apply((byte) 0);
    byte json = RPCFlag.BodyType.JSON.apply((byte) 0);
    byte zero = 0;
    assertEquals(RPCFlag.BodyType.UTF_8_STRING, RPCFlag.BodyType.extractBodyType(utf8String));
    assertEquals(RPCFlag.BodyType.JSON, RPCFlag.BodyType.extractBodyType(json));
    assertEquals(RPCFlag.BodyType.BINARY, RPCFlag.BodyType.extractBodyType(zero));
  }
}
