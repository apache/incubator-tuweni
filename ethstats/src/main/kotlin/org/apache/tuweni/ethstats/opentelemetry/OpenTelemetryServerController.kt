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
package org.apache.tuweni.ethstats.opentelemetry

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.Meter
import org.apache.tuweni.ethstats.BlockStats
import org.apache.tuweni.ethstats.EthStatsServerController

/**
 * Ethstats server controller for an OpenTelemetry meter.
 */
class OpenTelemetryServerController(meter: Meter) : EthStatsServerController {

  private val latencyGauge = meter.gaugeBuilder("ethstats.latency").ofLongs().setUnit("ms").buildObserver()

  private val latestBlockGauge = meter.gaugeBuilder("ethstats.latestBlock").ofLongs().buildObserver()

  private val pendingTxCount = meter.gaugeBuilder("ethstats.pendingTx").ofLongs().buildObserver()

  override fun readLatency(remoteAddress: String, id: String, latency: Long) {
    latencyGauge.record(
      latency,
      Attributes.of(AttributeKey.stringKey("address"), remoteAddress, AttributeKey.stringKey("id"), id)
    )
  }

  override fun readBlock(remoteAddress: String, id: String, block: BlockStats) {
    latestBlockGauge.record(
      block.number.toLong(),
      Attributes.of(AttributeKey.stringKey("address"), remoteAddress, AttributeKey.stringKey("id"), id)
    )
  }

  override fun readPendingTx(remoteAddress: String, id: String, pendingTx: Long) {
    pendingTxCount.record(
      pendingTx,
      Attributes.of(AttributeKey.stringKey("address"), remoteAddress, AttributeKey.stringKey("id"), id)
    )
  }
}
