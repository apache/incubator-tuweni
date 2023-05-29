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
package org.apache.tuweni.stratum

import io.vertx.core.Vertx
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.JSONRPCRequest
import org.apache.tuweni.eth.StringOrLong
import org.apache.tuweni.jsonrpc.JSONRPCClient
import org.apache.tuweni.stratum.server.PoWInput
import org.apache.tuweni.stratum.server.StratumServer
import org.apache.tuweni.units.bigints.UInt256
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

val logger = LoggerFactory.getLogger("stratum")

fun main(args: Array<String>) {
  if (args.size < 1) {
    println("[USAGE] port")
    exitProcess(1)
  }
  val vertx = Vertx.vertx()
  val client = JSONRPCClient(vertx, "http://localhost:8545", "")

  val port = args[0].toInt()
  val idCounter = AtomicLong(0)
  val seedReference = AtomicReference<Bytes32>()
  val server = StratumServer(
    vertx,
    port = port,
    networkInterface = "0.0.0.0",
    submitCallback = { solution ->
      logger.info("Got solution $solution")
      withContext(client.coroutineContext) {
        val req = JSONRPCRequest(
          id = StringOrLong(idCounter.incrementAndGet()),
          method = "eth_submitWork",
          params = arrayOf(
            Bytes.ofUnsignedLong(solution.nonce).toHexString(),
            solution.powHash.toHexString(),
            solution.mixHash.toHexString()
          )
        )
        logger.info("Sending work back to client $req")
        val response = client.sendRequest(req)

        val resp = response.await()
        logger.info("Received this response $resp")
        resp.result == true
      }
    },
    seedSupplier = { seedReference.get() },
    sslOptions = null
  )
  runBlocking {
    server.start()
  }
  server.launch {
    while (true) {
      server.launch innerloop@{
        try {
          val response = client.sendRequest(
            JSONRPCRequest(
              id = StringOrLong(idCounter.incrementAndGet()),
              method = "eth_getWork",
              params = arrayOf()
            )
          ).await()
          val error = response.error
          if (error != null) {
            logger.warn("Asking for work returned an error code ${error.code}: ${error.message}")
            return@innerloop
          }
          val result = response.result as List<*>
          val powHash = Bytes32.fromHexStringLenient(result[0] as String)
          val seed = Bytes32.fromHexStringLenient(result[1] as String)
          val difficulty = UInt256.fromBytes(Bytes.fromHexStringLenient(result[2] as String))
          val blockNumber = Bytes.fromHexStringLenient(result[3] as String).toLong()
          seedReference.set(seed)
          server.setNewWork(PoWInput(difficulty, powHash, blockNumber))
        } catch (t: Throwable) {
          logger.error(t.message, t)
        }
      }
      delay(5000)
    }
  }
  Runtime.getRuntime().addShutdownHook(
    Thread {
      runBlocking {
        logger.info("Shutting down...")
        server.stop()
        vertx.close()
      }
    }
  )
}
