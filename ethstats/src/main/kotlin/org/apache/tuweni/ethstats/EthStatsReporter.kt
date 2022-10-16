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
package org.apache.tuweni.ethstats

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketConnectOptions
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.eth.EthJsonModule
import org.apache.tuweni.units.bigints.UInt256
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.UncheckedIOException
import java.net.URI
import java.time.Instant
import java.util.Arrays
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext

/**
 * ETHNetStats reporting service.
 * <p>
 * This service connects to a running ethnetstats service and reports.
 * <p>
 * If the service is not available, the reporter will keep trying to connect periodically. The service will report
 * statistics over time.
 *
 * @param vertx a Vert.x instance, externally managed.
 * @param id the id of the ethstats reporter for communications
 * @param ethstatsServerURIs the URIs to connect to eth-netstats, such as ws://www.ethnetstats.org:3000/api. URIs are
 *        tried in sequence, and the first one to work is used.
 * @param secret the secret to use when we connect to eth-netstats
 * @param name the name of the node to be reported in the UI
 * @param node the node name to be reported in the UI
 * @param port the devp2p port exposed by this node
 * @param network the network id
 * @param protocol the version of the devp2p eth subprotocol, such as eth/63
 * @param os the operating system on which the node runs
 * @param osVer the version of the OS on which the node runs
 * @param historyRequester a hook for ethstats to request block information by number.
 * @param timeSupplier a function supplying time in milliseconds since epoch.
 * @param coroutineContext the coroutine context of the reporter.
 */
class EthStatsReporter(
  private val vertx: Vertx,
  private val id: String,
  private val ethstatsServerURIs: List<URI>,
  private val secret: String,
  private val name: String,
  private val node: String,
  private val port: Int,
  private val network: String,
  private val protocol: String,
  private val os: String,
  private val osVer: String,
  private val historyRequester: (List<UInt256>) -> Unit,
  private val timeSupplier: () -> Instant,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : CoroutineScope {

  companion object {
    val mapper = ObjectMapper()

    init {
      mapper.registerModule(EthJsonModule())
    }

    val logger = LoggerFactory.getLogger(EthStatsReporter::class.java)

    const val DELAY = 5000L
    const val REPORTING_PERIOD = 1000L
    const val PING_PERIOD = 15000L
  }

  val nodeInfo = NodeInfo(name, node, port, network, protocol, os = os, osVer = osVer)
  private val started = AtomicBoolean(false)
  private var client: HttpClient? = null
  private val newTxCount = AtomicReference<Int>()
  private val newHead = AtomicReference<BlockStats>()
  private val newNodeStats = AtomicReference<NodeStats>()
  private val newHistory = AtomicReference<List<BlockStats>>()
  private val waitingOnPong = AtomicBoolean(false)

  suspend fun start() {
    if (started.compareAndSet(false, true)) {
      client = vertx.createHttpClient(HttpClientOptions().setLogActivity(true))
      startInternal()
    }
  }

  suspend fun stop() {
    if (started.compareAndSet(true, false)) {
      logger.debug("Stopping the ethstats client service")
      client?.close()
    }
  }

  fun sendNewHead(newBlockStats: BlockStats) {
    newHead.set(newBlockStats)
  }

  fun sendNewPendingTransactionCount(txCount: Int) {
    newTxCount.set(txCount)
  }

  fun sendNewNodeStats(nodeStats: NodeStats) {
    newNodeStats.set(nodeStats)
  }

  fun sendHistoryResponse(blocks: List<BlockStats>) {
    newHistory.set(blocks)
  }

  private suspend fun startInternal() {
    for (uri in ethstatsServerURIs) {
      launch {
        attemptConnect(uri)
      }
    }
  }

  private suspend fun attemptConnect(uri: URI) {
    while (!connect(uri).await() && started.get()) {
      delay(DELAY)
    }
  }

  private suspend fun connect(uri: URI): AsyncResult<Boolean> {
    val result = AsyncResult.incomplete<Boolean>()
    val options = WebSocketConnectOptions().setHost(uri.host).setPort(uri.port)
      .setHeaders(MultiMap.caseInsensitiveMultiMap().add("origin", "http://localhost"))
    val ws = client!!.webSocket(options).await()
    ws.closeHandler { launch { attemptConnect(uri) } }
    ws.exceptionHandler { e ->
      logger.debug("Error while communicating with ethnetstats", e)
    }
    ws.textMessageHandler { message ->
      try {
        val node = mapper.readTree(message)
        val emitEvent = node.get("emit")
        if (emitEvent.isArray) {
          val eventValue = emitEvent.get(0).textValue()
          if (!result.isDone) {
            if ("ready" != eventValue) {
              logger.warn(message)
              result.complete(false)
            } else {
              logger.debug("Connected OK! {}", message)
              result.complete(true)

              // we are connected and now sending information
              reportPeriodically(uri, ws)
              writePing(ws)
              report(ws)
            }
          } else {
            handleEmitEvent(emitEvent as ArrayNode, ws)
          }
        } else {
          logger.warn(message)
          result.complete(false)
        }
      } catch (e: IOException) {
        throw UncheckedIOException(e)
      }
    }

    writeCommand(ws, "hello", AuthMessage(nodeInfo, id, secret))
    return result
  }

  private fun handleEmitEvent(event: ArrayNode, ws: WebSocket) {
    val command = event.get(0).textValue()
    when (command) {
      ("node-pong") -> {
        logger.debug("Received a pong {}", event.get(1))
        if (!waitingOnPong.compareAndSet(true, false)) {
          logger.warn("Received pong when we didn't expect one")
        } else {
          val start = event.get(1).get("clientTime").longValue()
          val latency = (Instant.now().toEpochMilli() - start) / (2 * 1000)
          writeCommand(ws, "latency", "latency", latency)
        }
      }
      "history" -> {
        logger.debug("History request {}", event.get(1))
        requestHistory(event.get(1))
      }
      else ->
        logger.warn("Unexpected message {}", command)
    }
  }

  private fun requestHistory(list: JsonNode) {
    val request = mutableListOf<UInt256>()
    for (elt in list) {
      request.add(UInt256.fromHexString(elt.asText()))
    }
    historyRequester(request)
  }

  private fun writePing(ws: WebSocket) {
    waitingOnPong.set(true)
    writeCommand(ws, "node-ping", "clientTime", timeSupplier())
  }

  private fun reportPeriodically(uri: URI, ws: WebSocket) {
    val reportingStream = vertx.periodicStream(REPORTING_PERIOD).handler {
      report(ws)
    }
    val pingStream = vertx.periodicStream(PING_PERIOD).handler {
      writePing(ws)
    }
    ws.closeHandler {
      reportingStream.cancel()
      pingStream.cancel()
      launch {
        attemptConnect(uri)
      }
    }
  }

  private fun report(ws: WebSocket) {
    val head = newHead.getAndSet(null)
    if (head != null) {
      writeCommand(ws, "block", "block", head)
    }
    val count = newTxCount.getAndSet(null)
    if (count != null) {
      writeCommand(ws, "pending", "stats", Collections.singletonMap("pending", count))
    }
    val nodeStats = newNodeStats.getAndSet(null)
    if (nodeStats != null) {
      writeCommand(ws, "stats", "stats", nodeStats)
    }
    val newBlocks = newHistory.getAndSet(null)
    if (newBlocks != null && !newBlocks.isEmpty()) {
      writeCommand(ws, "history", "history", newBlocks)
    }
  }

  private fun writeCommand(ws: WebSocket, command: String, payload: Any) {
    try {
      val message =
        mapper.writer().writeValueAsString(Collections.singletonMap("emit", Arrays.asList(command, payload)))
      logger.debug("Sending {} message {}", command, message)
      ws.writeTextMessage(message)
    } catch (e: JsonProcessingException) {
      throw UncheckedIOException(e)
    }
  }

  private fun writeCommand(ws: WebSocket, command: String, key: String, payload: Any) {
    val body = mutableMapOf<String, Any>()
    body["id"] = id
    body[key] = payload
    writeCommand(ws, command, body)
  }
}
