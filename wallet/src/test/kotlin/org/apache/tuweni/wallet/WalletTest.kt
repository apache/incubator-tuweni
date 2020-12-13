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
package org.apache.tuweni.wallet

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.TempDirectory
import org.apache.tuweni.junit.TempDirectoryExtension
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.nio.file.Path
import java.nio.file.Paths

@ExtendWith(TempDirectoryExtension::class, BouncyCastleExtension::class)
class WalletTest {

  @Test
  fun testCreate(@TempDirectory tempDir: Path) {
    val wallet = Wallet.create(tempDir.resolve(Paths.get("subfolder", "mywallet")), "password")
    val tx = wallet.sign(
      UInt256.valueOf(0),
      Wei.valueOf(3),
      Gas.valueOf(22),
      null,
      Wei.valueOf(2),
      Bytes.EMPTY,
      1
    )
    assertTrue(wallet.verify(tx))
  }

  @Test
  fun testCreateAndOpen(@TempDirectory tempDir: Path) {
    val wallet = Wallet.create(tempDir.resolve(Paths.get("subfolder", "mywallet")), "password")
    val tx = wallet.sign(
      UInt256.valueOf(0),
      Wei.valueOf(3),
      Gas.valueOf(22),
      null,
      Wei.valueOf(2),
      Bytes.EMPTY,
      1
    )
    val wallet2 = Wallet.open(tempDir.resolve(Paths.get("subfolder", "mywallet")), "password")
    assertTrue(wallet2.verify(tx))
  }
}
