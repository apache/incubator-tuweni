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
import org.apache.tuweni.ethclient.EthereumClient
import org.apache.tuweni.ethclient.EthereumClientConfig
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.HttpURLConnection
import java.net.URL

@ExtendWith(VertxExtension::class)
class UIIntegrationTest {

  @Test
  fun testServerComesUp(@VertxInstance vertx: Vertx) {
    val ui = UI(client = EthereumClient(vertx, EthereumClientConfig.fromString("[storage.forui]\npath=\"data\"")))
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
    ui.stop()
  }
}
