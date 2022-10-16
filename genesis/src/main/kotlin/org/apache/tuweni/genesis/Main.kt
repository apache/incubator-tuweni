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
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.units.bigints.UInt256
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.nio.file.Files
import java.nio.file.Paths
import java.security.Security

fun main(args: Array<String>) {
  Security.addProvider(BouncyCastleProvider())
  val config = QuorumConfig.generate(
    mixHash = Bytes32.random(),
    config = QuorumGenesisConfig(chainId = args[0].toInt()),
    numberValidators = 4,
    numberAllocations = 100,
    amount = UInt256.fromBytes(Bytes32.rightPad(Bytes.fromHexString("0x10")))
  )

  val mapper = JsonMapper()
  mapper.registerModule(EthJsonModule())
  val contents = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(config.genesis)
  Files.write(Paths.get("genesis.json"), contents)
  Files.write(Paths.get("accounts.csv"), config.allocsToCsv().toByteArray())
  Files.write(Paths.get("validators.csv"), config.validatorsToCsv().toByteArray())
}
