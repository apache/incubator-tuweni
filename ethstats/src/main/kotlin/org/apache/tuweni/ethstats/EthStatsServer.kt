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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.ServerWebSocket
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.eth.EthJsonModule
import org.slf4j.LoggerFactory
import java.io.IOException
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

class EthStatsServer(
  private val vertx: Vertx,
  val networkInterface: String,
  var port: Int,
  val secret: String,
  private val timeSupplier: () -> Instant = Instant::now,
  private val controller: EthStatsServerController,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : CoroutineScope {

  companion object {
    val logger = LoggerFactory.getLogger(EthStatsServer::class.java)
    val mapper = ObjectMapper()

    init {
      mapper.registerModule(EthJsonModule())
    }
  }

  private var server: HttpServer? = null
  private val started = AtomicBoolean(false)

  suspend fun start() {
    if (started.compareAndSet(false, true)) {
      server = vertx.createHttpServer().webSocketHandler(this::connect).exceptionHandler {
        logger.error("Exception occurred", it)
      }.listen(port, networkInterface).await()
    }
  }

  suspend fun stop() {
    if (started.compareAndSet(true, false)) {
      server?.close()?.await()
    }
  }

  fun connect(serverWebSocket: ServerWebSocket) {
    logger.debug("New connection")
    val websocket = serverWebSocket
    websocket.accept()
    websocket.exceptionHandler {
      logger.debug("Error reading message", it)
      websocket.close()
    }
    websocket.textMessageHandler {
      try {
        handleMessage(it, websocket)
      } catch (e: IOException) {
        logger.debug("Invalid payload", e)
        websocket.close()
      }
    }
  }

  private fun handleMessage(message: String, websocket: ServerWebSocket) {
    logger.debug("Received {}", message)
    val event = mapper.readTree(message).get("emit") as ArrayNode
    val command = event.get(0).textValue()
    when (command) {
      ("hello") -> {
        val clientSecret = event.get(1).get("secret").textValue()
        if (clientSecret != secret) {
          logger.info("Client {} connected with wrong secret {}, disconnecting", websocket.remoteAddress(), clientSecret)
          websocket.close()
          return
        }
        websocket.writeTextMessage("{\"emit\":[\"ready\"]}")
        val id = event.get(1).get("id").textValue()
        val nodeInfo = mapper.readerFor(NodeInfo::class.java).readValue<NodeInfo>(event.get(1).get("info"))
        controller.readNodeInfo(websocket.remoteAddress().toString(), id, nodeInfo)
        websocket.closeHandler {
          controller.readDisconnect(websocket.remoteAddress().toString(), id)
        }
      }
      ("node-ping") -> {
        logger.debug("Received a ping {}", event.get(1))
        val clientTime = event.get(1).get("clientTime").longValue()
        val payload = mapOf(Pair("clientTime", clientTime), Pair("serverTime", timeSupplier().toEpochMilli()))

        val response = mapper.writer().writeValueAsString(mapOf(Pair("emit", listOf("node-pong", payload))))
        EthStatsReporter.logger.debug("Sending {} message {}", command, message)
        websocket.writeTextMessage(response)
      }
      ("latency") -> {
        val id = event.get(1).get("id").textValue()
        val latency = event.get(1).get("latency").longValue()
        controller.readLatency(websocket.remoteAddress().toString(), id, latency)
      }
      ("end") -> {
        val id = event.get(1).get("id").textValue()
        controller.readDisconnect(websocket.remoteAddress().toString(), id)
      }
      ("stats") -> {
        val id = event.get(1).get("id").textValue()
        val nodeStats = mapper.readerFor(NodeStats::class.java).readValue<NodeStats>(event.get(1).get("stats"))
        controller.readNodeStats(websocket.remoteAddress().toString(), id, nodeStats)
      }
      ("pending") -> {
        val id = event.get(1).get("id").textValue()
        val pendingTx = event.get(1).get("stats").get("pending").longValue()
        controller.readPendingTx(websocket.remoteAddress().toString(), id, pendingTx)
      }
      ("block") -> {
        val id = event.get(1).get("id").textValue()
        val block = mapper.readerFor(BlockStats::class.java).readValue<BlockStats>(event.get(1).get("block"))
        controller.readBlock(websocket.remoteAddress().toString(), id, block)
      }
      ("update") -> {
        // TODO implement
      }
      ("history") -> {
        // TODO implement
      }
      else -> {
        logger.error("Unknown command: {}", message)
        websocket.close()
      }
    }
  }
}
