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
