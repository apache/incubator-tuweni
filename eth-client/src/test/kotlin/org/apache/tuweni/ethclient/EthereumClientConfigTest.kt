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
package org.apache.tuweni.ethclient

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException
import java.nio.file.Paths

class EthereumClientConfigTest {

  @Test
  fun testFileConfig() {
    val config =
      EthereumClientConfig.fromFile(Paths.get(EthereumClientConfigTest::class.java.getResource("/minimal.conf").path))
    assertNotNull(config)
  }

  @Test
  fun testInvalidFileConfig() {
    val exception: IllegalArgumentException = assertThrows {
      EthereumClientConfig.fromFile(Paths.get("foo"))
    }
    assertEquals("Missing config file: 'foo'", exception.message)
  }

  @Test
  fun testEmptyConfig() {
    val config = EthereumClientConfig.fromString("")
    assertNotNull(config)
  }

  @Test
  fun testEmptyConfigHasOnePeerRepository() {
    val config = EthereumClientConfig.fromString("")
    assertEquals(1, config.peerRepositories().size)
    val peerRepo = config.peerRepositories()[0]
    assertEquals("default", peerRepo.getName())
  }

  @Test
  fun testEmptyConfigHasOneWireRepository() {
    val config = EthereumClientConfig.fromString("")
    assertEquals(1, config.rlpxServices().size)
    val rlpx = config.rlpxServices()[0]
    assertEquals("default", rlpx.getName())
  }

  @Test
  fun testEmptyConfigHasOneDataStorage() {
    val config = EthereumClientConfig.fromString("")
    assertEquals(1, config.dataStores().size)
    val store = config.dataStores()[0]
    assertEquals("default", store.getName())
    assertEquals(Paths.get("data"), store.getStoragePath())
  }

  @Test
  fun testDefinedStorageTakesTheDefaultSpot() {
    val config = EthereumClientConfig.fromString("[storage.mine]\npath=\"data2\"\ngenesis=\"default\"")
    assertEquals(1, config.dataStores().size)
    val store = config.dataStores()[0]
    assertEquals("mine", store.getName())
    assertEquals(Paths.get("data2"), store.getStoragePath())
  }
}
