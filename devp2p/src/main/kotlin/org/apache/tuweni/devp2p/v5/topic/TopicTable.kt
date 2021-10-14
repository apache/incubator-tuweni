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
package org.apache.tuweni.devp2p.v5.topic

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import org.apache.tuweni.devp2p.DiscoveryService
import org.apache.tuweni.devp2p.EthereumNodeRecord
import java.util.concurrent.TimeUnit

internal class TopicTable(
  private val tableCapacity: Int = MAX_TABLE_CAPACITY,
  private val queueCapacity: Int = MAX_ENTRIES_PER_TOPIC
) {

  private val timeSupplier: () -> Long = DiscoveryService.CURRENT_TIME_SUPPLIER
  private val table: HashMap<Topic, Cache<String, TargetAd>> = HashMap(tableCapacity)

  init {
    require(tableCapacity > 0) { "Table capacity value must be positive" }
    require(queueCapacity > 0) { "Queue capacity value must be positive" }
  }

  fun getNodes(topic: Topic): List<EthereumNodeRecord> {
    val values = table[topic]
    return values?.let { values.asMap().values.map { it.enr } } ?: emptyList()
  }

  /**
   * Puts a topic in a table
   *
   * @return wait time for registrant node (0 is topic was putted immediately, -1 in case of error)
   */
  @Synchronized
  fun put(topic: Topic, enr: EthereumNodeRecord): Long {
    gcTable()

    val topicQueue = table[topic]
    val nodeId = enr.nodeId().toHexString()

    if (null != topicQueue) {
      if (topicQueue.size() < queueCapacity) {
        topicQueue.put(nodeId, TargetAd(timeSupplier(), enr))
        return 0 // put immediately
      } else {
        // Queue if full (wait time = target-ad-lifetime - oldest ad lifetime in queue)
        return TARGET_AD_LIFETIME_MS - (timeSupplier() - topicQueue.oldest().regTime)
      }
    }

    if (table.size < tableCapacity) {
      table[topic] = createNewQueue().apply { put(nodeId, TargetAd(timeSupplier(), enr)) }
      return 0 // put immediately
    } else {
      // table is full (wait time = target-ad-lifetime - oldest in table of youngest in queue)
      val oldestInTable = table.entries.map { it.value.youngest().regTime }.minOrNull() ?: -1
      return TARGET_AD_LIFETIME_MS - (timeSupplier() - oldestInTable)
    }
  }

  fun contains(topic: Topic): Boolean = table.containsKey(topic)

  fun isEmpty(): Boolean = table.isEmpty()

  fun clear() = table.clear()

  private fun createNewQueue(): Cache<String, TargetAd> {
    return CacheBuilder.newBuilder()
      .expireAfterWrite(TARGET_AD_LIFETIME_MS, TimeUnit.MILLISECONDS)
      .initialCapacity(queueCapacity)
      .build()
  }

  private fun gcTable() {
    table.entries.removeIf { it.value.size() == 0L }
  }

  private fun Cache<String, TargetAd>.oldest(): TargetAd {
    return asMap().values.minByOrNull { it.regTime } ?: throw IllegalArgumentException(QUEUE_EMPTY_MSG)
  }

  private fun Cache<String, TargetAd>.youngest(): TargetAd {
    return asMap().values.maxByOrNull { it.regTime } ?: throw IllegalArgumentException(QUEUE_EMPTY_MSG)
  }

  companion object {
    internal const val MAX_ENTRIES_PER_TOPIC: Int = 100
    private const val MAX_TABLE_CAPACITY: Int = 500
    private const val TARGET_AD_LIFETIME_MS = (15 * 60 * 1000).toLong() // 15 min

    private const val QUEUE_EMPTY_MSG = "Queue is empty."
  }
}

internal class TargetAd(val regTime: Long, val enr: EthereumNodeRecord)
