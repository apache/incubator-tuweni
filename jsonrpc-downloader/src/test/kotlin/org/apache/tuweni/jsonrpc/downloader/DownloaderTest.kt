// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
      """.trimIndent(),
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
