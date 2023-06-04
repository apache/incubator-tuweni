// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.crawler

import com.nhaarman.mockitokotlin2.mock
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class CrawlerRESTServiceTest {

  @Test
  fun startAndStop() = runBlocking {
    val repo = mock<RelationalPeerRepository>()
    val statsMeter = SdkMeterProvider.builder().build().get("stats")
    val meter = SdkMeterProvider.builder().build().get("crawler")
    val stats = StatsJob(repo, listOf(), statsMeter)

    val service = CrawlerRESTService(repository = repo, meter = meter, stats = stats)
    service.start().await()
    service.stop().await()
    Unit
  }
}
