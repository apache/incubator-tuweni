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
package org.apache.tuweni.devp2p.eth

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.units.bigints.UInt256
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
class BlockchainInformationTest {
  @Test
  fun testForkHashes() {
    val contents = BlockchainInformationTest::class.java.getResourceAsStream("/mainnet.json").readAllBytes()
    val genesisFile = GenesisFile.read(contents)
    val genesisBlock = genesisFile.toBlock()
    val info = SimpleBlockchainInformation(
      UInt256.valueOf(genesisFile.chainId.toLong()), genesisBlock.header.difficulty,
      genesisBlock.header.hash, UInt256.valueOf(42L), genesisBlock.header.hash, genesisFile.forks
    )
    assertEquals(Bytes.fromHexString("0xfc64ec04"), info.getForkHashes()[0])
    assertEquals(Bytes.fromHexString("0x97c2c34c"), info.getForkHashes()[1])
    assertEquals(Bytes.fromHexString("0x91d1f948"), info.getForkHashes()[2])
    assertEquals(Bytes.fromHexString("0x7a64da13"), info.getForkHashes()[3])
    assertEquals(Bytes.fromHexString("0x3edd5b10"), info.getForkHashes()[4])
    assertEquals(Bytes.fromHexString("0xa00bc324"), info.getForkHashes()[5])
    assertEquals(Bytes.fromHexString("0x668db0af"), info.getForkHashes()[6])
    assertEquals(Bytes.fromHexString("0xe029e991"), info.getLatestForkHash())
  }

  @Test
  fun testLatestFork() {
    val contents = BlockchainInformationTest::class.java.getResourceAsStream("/mainnet.json").readAllBytes()
    val genesisFile = GenesisFile.read(contents)
    val genesisBlock = genesisFile.toBlock()
    val info = SimpleBlockchainInformation(
      UInt256.valueOf(genesisFile.chainId.toLong()), genesisBlock.header.difficulty,
      genesisBlock.header.hash, UInt256.valueOf(42L), genesisBlock.header.hash, genesisFile.forks
    )
    assertEquals(9200000L, info.getLatestFork())
  }
}
