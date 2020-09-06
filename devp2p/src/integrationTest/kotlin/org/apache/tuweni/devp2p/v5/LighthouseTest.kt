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
package org.apache.tuweni.devp2p.v5

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.InetSocketAddress

/**
 * Test a developer can run from their machine to contact a remote server.
 */
@ExtendWith(BouncyCastleExtension::class)
class LighthouseTest {

  @Disabled
  @Test
  fun testConnect() = runBlocking {
    val enrRec =
      "-Iu4QAroHG8hI2LqSxVgkgaKPaFrtz8qjB5ODbT9Ca0ch2bzS_yZE5ermtfCS0A0LbWBYi7E6SZUE3J" +
        "HHtwtn4tcwdcBgmlkgnY0gmlwhH8AAAGJc2VjcDI1NmsxoQMIVIz0gYvB0yDDz4E7n01YJoLlCTJj" +
        "VCA4RiKQ54M6aYN0Y3CCIyiDdWRwgiMo"
//    val record =
//      EthereumNodeRecord.fromRLP(Base64URLSafe.decode(enrRec))

    val service = DiscoveryService.open(
      SECP256K1.KeyPair.random(),
      localPort = 0,
      bindAddress = InetSocketAddress("0.0.0.0", 10000),
      bootstrapENRList = listOf(enrRec)
    )
    service.start()
    kotlinx.coroutines.delay(50000)
  }
}
