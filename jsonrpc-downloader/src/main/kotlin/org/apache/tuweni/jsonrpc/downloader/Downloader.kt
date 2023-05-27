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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.buffer.Buffer
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.app.commons.ApplicationUtils
import org.apache.tuweni.jsonrpc.JSONRPCClient
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.Exception
import java.lang.Integer.max
import java.lang.Integer.min
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.Security
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.math.round
import kotlin.system.exitProcess

val logger = LoggerFactory.getLogger(Downloader::class.java)

/**
 * Application downloading chain data from a JSON-RPC endpoint.
 */
object DownloaderApp {

  @JvmStatic
  fun main(args: Array<String>) {
    runBlocking {
      if (args.contains("--version")) {
        println("Apache Tuweni JSON-RPC downloader ${ApplicationUtils.version}")
        exitProcess(0)
      }
      if (args.contains("--help") || args.contains("-h")) {
        println("USAGE: jsonrpc-downloader <config file>")
        exitProcess(0)
      }
      ApplicationUtils.renderBanner("Loading JSON-RPC downloader")
      Security.addProvider(BouncyCastleProvider())
      val configFile = Paths.get(if (args.isNotEmpty()) args[0] else "config.toml")
      Security.addProvider(BouncyCastleProvider())

      val config = DownloaderConfig(configFile)
      if (config.config.hasErrors()) {
        for (error in config.config.errors()) {
          println(error.message)
        }
        System.exit(1)
      }
      val vertx = Vertx.vertx(VertxOptions().setWorkerPoolSize(config.numberOfThreads()))
      val pool = Executors.newFixedThreadPool(
        config.numberOfThreads()
      ) {
        val thread = Thread("downloader")
        thread.isDaemon = true
        thread
      }
      val downloader = Downloader(vertx, config, pool.asCoroutineDispatcher())
      logger.info("Starting download")
      try {
        downloader.loopDownload()
      } catch (e: Exception) {
        logger.error("Fatal error downloading blocks", e)
        exitProcess(1)
      }
      logger.info("Completed download")

      vertx.close()
      pool.shutdown()
    }
  }
}

class Downloader(val vertx: Vertx, val config: DownloaderConfig, override val coroutineContext: CoroutineContext) :
  CoroutineScope {

  val jsonRpcClient: JSONRPCClient
  val objectMapper = ObjectMapper()

  init {
    jsonRpcClient = JSONRPCClient(vertx, config.url(), coroutineContext = this.coroutineContext)
  }

  suspend fun loopDownload() = coroutineScope {
    val state = readInitialState()
    val intervals = createMissingIntervals(state)
    logger.info("Working with intervals $intervals")
    val jobs = mutableListOf<Job>()
    var length = 0
    var completed = 0
    for (interval in intervals) {
      length += interval.last - interval.first
      for (i in interval) {
        val job = launch {
          try {
            val block = downloadBlock(i)
            writeBlock(i, block)
          } catch (e: Exception) {
            logger.error("Error downloading block $i, aborting", e)
          }
          completed++
        }
        jobs.add(job)
      }
    }
    launch {
      while (completed < length) {
        delay(5000)
        logger.info("Progress ${round(completed * 100.0 * 100 / length) / 100}")
      }
    }
    jobs.joinAll()
    writeFinalState()
  }

  private suspend fun downloadBlock(blockNumber: Int): String {
    val blockJson = jsonRpcClient.getBlockByBlockNumber(blockNumber, true)
    return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(blockJson)
  }

  private suspend fun writeBlock(blockNumber: Int, block: String) {
    val filePath = Paths.get(config.outputPath(), "block-${blockNumber.toString().padStart(16, '0')}.json")
    coroutineScope {
      vertx.fileSystem().writeFile(filePath.toString(), Buffer.buffer(block)).await()
    }
  }

  fun createMissingIntervals(state: DownloadState): List<IntRange> {
    val intervals = mutableListOf<IntRange>()
    if (config.start() < state.start) {
      intervals.add(config.start()..min(state.start, config.end()))
    }
    if (state.end < config.end()) {
      intervals.add(max(config.start(), state.end)..config.end())
    }

    return intervals
  }

  private fun readInitialState(): DownloadState {
    // read the initial state
    var initialState = DownloadState(0, 0)
    try {
      val str = Files.readString(Path.of(config.outputPath(), ".offset"))
      initialState = objectMapper.readValue(str, object : TypeReference<DownloadState>() {})
    } catch (e: IOException) {
      // ignored
    }
    return initialState
  }

  private fun writeFinalState() {
    val state = DownloadState(config.start(), config.end())
    val json = objectMapper.writeValueAsString(state)
    Files.writeString(Path.of(config.outputPath(), ".offset"), json)
  }
}
