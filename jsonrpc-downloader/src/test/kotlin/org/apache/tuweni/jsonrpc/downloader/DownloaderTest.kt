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
package org.apache.tuweni.jsonrpc.downloader

import io.vertx.core.Vertx
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(VertxExtension::class)
class DownloaderTest {
  @Test
  fun testIntervals(@VertxInstance vertx: Vertx) {
    val config = DownloaderConfig(
      configContents = """
      start=10
      end=20
      url="example.com"
      """.trimIndent()
    )
    val downloader = Downloader(vertx, config, Dispatchers.Default)
    var intervals = downloader.createMissingIntervals(DownloadState(0, 0))
    assertEquals(1, intervals.size)
    assertEquals(10..20, intervals[0])
    intervals = downloader.createMissingIntervals(DownloadState(10, 20))
    assertEquals(0, intervals.size)
    intervals = downloader.createMissingIntervals(DownloadState(25, 40))
    assertEquals(1, intervals.size)
    assertEquals(10..20, intervals[0])
    intervals = downloader.createMissingIntervals(DownloadState(5, 15))
    assertEquals(1, intervals.size)
    assertEquals(15..20, intervals[0])
    intervals = downloader.createMissingIntervals(DownloadState(15, 25))
    assertEquals(1, intervals.size)
    assertEquals(10..15, intervals[0])
    intervals = downloader.createMissingIntervals(DownloadState(12, 18))
    assertEquals(2, intervals.size)
    assertEquals(10..12, intervals[0])
    assertEquals(18..20, intervals[1])
  }
}
