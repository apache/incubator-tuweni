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
package org.apache.tuweni.genesis

import com.fasterxml.jackson.databind.json.JsonMapper
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.eth.genesis.GenesisFile
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.units.bigints.UInt256
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
class QuorumTest {

  @Test
  fun testGenerateQuorumGenesis() {
    val quorum = QuorumConfig.generate(
      nonce = Bytes.fromHexString("0xdeadbeef"),
      difficulty = UInt256.ONE.shiftLeft(252),
      mixHash = Bytes32.leftPad(Bytes.fromHexString("0xf000")),
      coinbase = Address.fromHexString("0xdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef"),
      timestamp = 0L,
      gasLimit = 0L,
      parentHash = Bytes32.leftPad(Bytes.fromHexString("0x00ff")),
      config = QuorumGenesisConfig(chainId = 1337),
      numberAllocations = 10,
      amount = UInt256.valueOf(123),
      numberValidators = 4,
      vanity = Bytes32.leftPad(Bytes.fromHexString("0xdeadbeef"))
    )
    val genesis = quorum.genesis
    assertEquals(12, quorum.allocsToCsv().split("\n").size)
    assertEquals("User,Public key,Address,Secret key", quorum.allocsToCsv().split("\n")[0])
    val mapper = JsonMapper()
    mapper.registerModule(EthJsonModule())
    val contents = mapper.writeValueAsBytes(genesis)
    GenesisFile.read(contents)
  }
}
