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
package org.apache.tuweni.ethclientui

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.ethclient.EthereumClient
import org.apache.tuweni.ethclient.EthereumClientConfig
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.TempDirectory
import org.apache.tuweni.junit.TempDirectoryExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Path

@ExtendWith(VertxExtension::class, BouncyCastleExtension::class, TempDirectoryExtension::class)
class UIIntegrationTest {

  @Test
  fun testServerComesUp(@VertxInstance vertx: Vertx, @TempDirectory tempDir: Path) = runBlocking {
    val ui = UI(
      client = EthereumClient(
        vertx,
        EthereumClientConfig.fromString(
          """[storage.default]
path="${tempDir.toAbsolutePath()}"
genesis="default"
[genesis.default]
path="classpath:/genesis/dev.json"
[peerRepository.default]
type="memory""""
        )
      )
    )
    ui.client.start()
    ui.start()
    val url = URL("http://localhost:" + ui.actualPort)
    val con = url.openConnection() as HttpURLConnection
    con.requestMethod = "GET"
    val response = con.inputStream.readAllBytes()
    assertTrue(response.isNotEmpty())

    val url2 = URL("http://localhost:" + ui.actualPort + "/rest/config")
    val con2 = url2.openConnection() as HttpURLConnection
    con2.requestMethod = "GET"
    val response2 = con2.inputStream.readAllBytes()
    assertTrue(response2.isNotEmpty())
    val url3 = URL("http://localhost:" + ui.actualPort + "/rest/state")
    val con3 = url3.openConnection() as HttpURLConnection
    con3.requestMethod = "GET"
    val response3 = con3.inputStream.readAllBytes()
    assertTrue(response3.isNotEmpty())
    assertEquals("""{"peerCounts":{"default":0},"bestBlocks":{"default":{"hash":"0xa08d1edb37ba1c62db764ef7c2566cbe368b850f5b3762c6c24114a3fd97b87f","number":"0x0000000000000000000000000000000000000000000000000000000000000000"}}}""", String(response3))
    ui.stop()
  }
}
