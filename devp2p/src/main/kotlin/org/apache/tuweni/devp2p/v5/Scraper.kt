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
package org.apache.tuweni.devp2p.v5

import io.vertx.core.Vertx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.ExpiringSet
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.io.Base64URLSafe
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.net.InetSocketAddress
import java.security.Security
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Wrapper to run the scraper as an app.
 */
object ScraperApp {

  @JvmStatic
  fun main(args: Array<String>) {
    Security.addProvider(BouncyCastleProvider())
    run(args)
  }

  fun run(args: Array<String>) {
    val enrs = args.map { EthereumNodeRecord.fromRLP(Base64URLSafe.decode(it)) }
    val addr = InetSocketAddress("0.0.0.0", 10000)
    val seen = ConcurrentHashMap.newKeySet<EthereumNodeRecord>()
    val scraper = Scraper(
      initialENRs = enrs,
      bindAddress = addr,
      listeners = listOf { _, nodes ->
        for (node in nodes) {
          if (seen.add(node)) {
            println(
              node.ip().hostAddress + "," + node.udp() + "," + node.data["attnets"] + "," +
                Base64URLSafe.encode(node.toRLP())
            )
          }
        }
      }
    )
    Runtime.getRuntime().addShutdownHook(
      Thread {
        runBlocking {
          scraper.stop().await()
        }
      }
    )
    runBlocking {
      scraper.start().await()
    }
  }
}

/**
 * Discovery scraper that will continue asking peers for peers, and iterate over them, until told to stop.
 *
 * The scraper sends events of discoveries to listeners.
 */
class Scraper(
  override val coroutineContext: CoroutineContext = Executors.newFixedThreadPool(100).asCoroutineDispatcher(),
  val vertx: Vertx = Vertx.vertx(),
  val initialENRs: List<EthereumNodeRecord>,
  val bindAddress: InetSocketAddress,
  val listeners: List<(EthereumNodeRecord, List<EthereumNodeRecord>) -> Unit>,
  val maxWaitForNewPeers: Long = 20L,
  val waitBetweenScrapes: Long = (5 * 60).toLong()
) : CoroutineScope {

  private var service: DiscoveryV5Service? = null
  private val started = AtomicBoolean(false)

  fun start() = async {
    val newService = DiscoveryService.open(
      coroutineContext = coroutineContext,
      vertx = vertx,
      keyPair = SECP256K1.KeyPair.random(),
      localPort = 0,
      bindAddress = bindAddress,
      bootstrapENRList = emptyList()
    )
    newService.start().await()
    for (enr in initialENRs) {
      newService.addPeer(enr).await()
    }
    service = newService
    started.set(true)
    while (started.get()) {
      discover(maxWaitForNewPeers).await()
      delay(waitBetweenScrapes * 1000)
    }
  }

  fun discover(maxWaitForNewPeers: Long) = async {
    var newPeersDetected = true
    val nodes = ExpiringSet<EthereumNodeRecord>(24 * 60 * 60 * 1000L)
    while (newPeersDetected) {
      newPeersDetected = false
      for (i in 1..255) {
        service?.requestNodes(i)?.thenAccept {
          for (node in it.entries) {
            if (node.value.isNotEmpty()) {
              for (listener in listeners) {
                launch {
                  listener(node.key, node.value)
                }
              }
              for (newENR in node.value) {
                if (nodes.add(newENR)) {
                  newPeersDetected = true
                  launch {
                    service?.addPeer(newENR)
                  }
                }
              }
            }
          }
        }
      }
      delay(maxWaitForNewPeers * 1000)
    }
  }

  fun stop() = async {
    if (started.compareAndSet(true, false)) {
      val s = service
      service = null
      s!!.terminate()
    }
  }
}
