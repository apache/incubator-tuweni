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

import static org.apache.tuweni.eth.BlockHeaderTest.generateBlockHeader;
import static org.apache.tuweni.eth.TransactionTest.generateTransaction;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.junit.BouncyCastleExtension;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class BlockTest {

  @Test
  void testRoundtripRLP() {
    Block block = new Block(
        generateBlockHeader(),
        new BlockBody(
            Arrays.asList(generateTransaction(), generateTransaction(), generateTransaction(), generateTransaction()),
            Arrays
                .asList(
                    generateBlockHeader(),
                    generateBlockHeader(),
                    generateBlockHeader(),
                    generateBlockHeader(),
                    generateBlockHeader(),
                    generateBlockHeader())));
    Bytes encoded = block.toBytes();
    Block read = Block.fromBytes(encoded);
    assertEquals(block, read);
  }
}
