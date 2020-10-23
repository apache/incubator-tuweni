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
package org.apache.tuweni.devp2p

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.ExpiringSet
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
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
    val uris = args.sliceArray(1..args.size - 1).map { URI.create(it) }
    val addr = InetSocketAddress("0.0.0.0", 11000)
    val seen = ConcurrentHashMap.newKeySet<String>()
    val scraper = Scraper(initialURIs = uris,
      bindAddress = addr,
      advertiseAddress = InetAddress.getByName(args[0]),
      listeners = listOf { _, nodes ->
      for (node in nodes) {
        if (seen.add(node.uri())) {
          println(node)
        }
      }
    })
    Runtime.getRuntime().addShutdownHook(Thread {
      runBlocking {
        scraper.stop().await()
      }
    })
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
  val initialURIs: List<URI>,
  val bindAddress: InetSocketAddress,
  val advertiseAddress: InetAddress,
  val listeners: List<(Peer, List<Peer>) -> Unit>,
  val maxWaitForNewPeers: Long = 20,
  val waitBetweenScrapes: Long = 5 * 60
) : CoroutineScope {

  private var service: DiscoveryService? = null
  private val started = AtomicBoolean(false)

  fun start() = async {
    val newService = DiscoveryService.open(
      keyPair = SECP256K1.KeyPair.random(),
      bindAddress = bindAddress,
      bootstrapURIs = initialURIs,
      advertiseAddress = advertiseAddress
    )
    service = newService
    started.set(true)
    while (started.get()) {
      discover(maxWaitForNewPeers).await()
      delay(waitBetweenScrapes * 1000)
    }
  }

  fun discover(maxWaitForNewPeers: Long) = async {
    var newPeersDetected = true
    val nodes = ExpiringSet<Peer>(24 * 60 * 60 * 1000)
    while (newPeersDetected) {
      newPeersDetected = false
      for (node in nodes) {
        service?.lookupAsync(node.nodeId)?.thenAccept {
          if (it.isNotEmpty()) {
            for (listener in listeners) {
              launch {
                listener(node, it)
              }
            }
            for (newPeer in it) {
              if (nodes.add(newPeer)) {
                newPeersDetected = true
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
      s!!.shutdown()
      s.awaitTermination()
    }
  }
}
