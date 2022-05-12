/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.eth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.ethereum.Wei;

import org.junit.jupiter.api.Test;

class AccountStateTest {

  @Test
  void roundtripRLP() {
    AccountState state = new AccountState(
        UInt256.ONE,
        Wei.valueOf(32L),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        2);
    Bytes message = state.toBytes();
    assertEquals(state, AccountState.fromBytes(message));
  }

  @Test
  void fromBytes() {
    AccountState state = AccountState
        .fromBytes(
            Bytes
                .fromHexString(
                    "0xf84d80893635c9adc5de99bb50a056e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421a0c5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470"));
    assertEquals(
        new AccountState(
            UInt256.ZERO,
            Wei.valueOf(UInt256.fromHexString("0x00000000000000000000000000000000000000000000003635c9adc5de99bb50")),
            Hash.fromHexString("0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421"),
            Hash.fromHexString("0xc5d2460186f7233c927e7db2dcc703c0e500b653ca82273b7bfad8045d85a470"),
            0),
        state);
  }
}
