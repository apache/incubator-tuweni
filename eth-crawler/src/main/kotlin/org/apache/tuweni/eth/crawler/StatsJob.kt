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

import io.opentelemetry.api.metrics.Meter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class StatsJob(
  private val repo: RelationalPeerRepository,
  private val upgradeConfigs: List<UpgradeConfig> = listOf(),
  private val meter: Meter,
  private val clientsStatsDelay: Long = 30 * 1000L,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : CoroutineScope {

  companion object {
    internal val logger = LoggerFactory.getLogger(StatsJob::class.java)
  }

  private var clientIds: List<ClientInfo>? = null
  private var clientsStats: Map<String, Map<String, Long>>? = null
  private var upgradeStats: MutableMap<String, ClientReadyStats> = mutableMapOf()
  private val started = AtomicBoolean(false)
  private var job: Job? = null

  private val totalClientsGauge =
    meter.longValueRecorderBuilder("totalClients").setDescription("Number of nodes used to compute client stats")
      .build()
  private val clientCalculationsCounter =
    meter.longCounterBuilder("clients").setDescription("Number of times clients were computed").build()

  fun start() {
    logger.info("Starting stats job")
    job = launch(coroutineContext, CoroutineStart.UNDISPATCHED) {
      logger.info("Computing client ids")
      started.set(true)
      while (started.get()) {
        try {
          runStats()
        } catch (e: Exception) {
          logger.error("Error while computing stats", e)
        }

        delay(clientsStatsDelay)
      }
    }
  }

  private fun runStats() {
    logger.info("runStats")
    val newClientIds = repo.getClientIdsInternal()
    logger.info("Found client ids ${newClientIds.size}")
    clientIds = newClientIds
    val newClientsStats = mutableMapOf<String, MutableMap<String, Long>>()
    val total = newClientIds.stream().mapToInt { it.count }.sum()

    newClientIds.forEach { newClientCount ->
      val clientIdInfo = ClientIdInfo(newClientCount.clientId)
      val versionStats = newClientsStats.computeIfAbsent(clientIdInfo.name) { mutableMapOf() }
      val statsCount = versionStats[clientIdInfo.version] ?: 0
      versionStats[clientIdInfo.version] = statsCount + newClientCount.count
    }
    for (upgradeConfig in upgradeConfigs) {
      var upgradeReady = 0
      newClientIds.forEach { newClientCount ->
        val clientIdInfo = ClientIdInfo(newClientCount.clientId)
        upgradeConfig.versions.get(clientIdInfo.name().lowercase())?.let { upgradeVersion ->
          if (clientIdInfo >= upgradeVersion) {
            upgradeReady += newClientCount.count
          }
        }
      }
      upgradeStats.put(upgradeConfig.name, ClientReadyStats(total, upgradeReady))
    }
    clientsStats = newClientsStats
    totalClientsGauge.record(total.toLong())
    clientCalculationsCounter.add(1)
  }

  suspend fun stop() {
    started.set(false)
    job?.cancel()
    job?.join()
  }

  internal fun getUpgradeStats() = upgradeStats

  internal fun getClientIds(): List<ClientInfo> = clientIds ?: listOf()

  internal fun getClientStats(): Map<String, Map<String, Long>> = clientsStats ?: mapOf()
}
