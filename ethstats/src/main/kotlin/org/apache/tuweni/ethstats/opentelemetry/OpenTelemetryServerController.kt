// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
      Attributes.of(AttributeKey.stringKey("address"), remoteAddress, AttributeKey.stringKey("id"), id),
    )
  }

  override fun readBlock(remoteAddress: String, id: String, block: BlockStats) {
    latestBlockGauge.record(
      block.number.toLong(),
      Attributes.of(AttributeKey.stringKey("address"), remoteAddress, AttributeKey.stringKey("id"), id),
    )
  }

  override fun readPendingTx(remoteAddress: String, id: String, pendingTx: Long) {
    pendingTxCount.record(
      pendingTx,
      Attributes.of(AttributeKey.stringKey("address"), remoteAddress, AttributeKey.stringKey("id"), id),
    )
  }
}
