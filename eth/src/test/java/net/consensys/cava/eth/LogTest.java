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
package net.consensys.cava.eth;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.rlp.RLP;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class LogTest {

  @Test
  void testRLProundtrip() {
    Log log = new Log(
        Address.fromBytes(Bytes.random(20)),
        Bytes.of(1, 2, 3),
        Arrays.asList(Bytes32.random(), Bytes32.random()));
    Bytes rlp = RLP.encode(log::writeTo);
    Log read = RLP.decode(rlp, Log::readFrom);
    assertEquals(log, read);
  }
}
