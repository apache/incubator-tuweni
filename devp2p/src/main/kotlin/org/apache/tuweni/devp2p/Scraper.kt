// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p

import io.vertx.core.Vertx
import io.vertx.core.net.SocketAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.ExpiringSet
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.net.URI
import java.security.Security
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
    val uris = args.map { URI.create(it) }
    val addr = SocketAddress.inetSocketAddress(11000, "0.0.0.0")
    val scraper = Scraper(
      initialURIs = uris,
      bindAddress = addr,
      repository = EphemeralPeerRepository(),
      listeners = listOf {
        println(it.uri())
      },
    )
    Runtime.getRuntime().addShutdownHook(
      Thread {
        runBlocking {
          scraper.stop().await()
        }
      },
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
  val initialURIs: List<URI>,
  val bindAddress: SocketAddress,
  val repository: PeerRepository,
  val listeners: List<(Peer) -> Unit>? = null,
  val waitSecondsBetweenScrapes: Long = 30,
) : CoroutineScope {

  private var service: DiscoveryService? = null
  private val started = AtomicBoolean(false)
  private val nodes = ExpiringSet<Peer>(24 * 60 * 60 * 1000L)

  fun start() = async {
    repository.addListener {
      if (nodes.add(it)) {
        if (listeners != null) {
          for (listener in listeners) {
            listener(it)
          }
        }
      }
    }
    val newService = DiscoveryService.open(
      vertx,
      keyPair = SECP256K1.KeyPair.random(),
      bindAddress = bindAddress,
      bootstrapURIs = initialURIs,
      peerRepository = repository,
    )
    service = newService
    newService.awaitBootstrap()
    started.set(true)
    while (started.get()) {
      discover().await()
      delay(waitSecondsBetweenScrapes * 1000)
    }
  }

  fun discover() = async {
    for (node in nodes) {
      service?.lookupAsync(node.nodeId)?.thenAccept {
        for (newNode in it) {
          if (nodes.add(newNode)) {
            if (listeners != null) {
              for (listener in listeners) {
                listener(newNode)
              }
            }
          }
        }
      }
    }

    while (started.get()) {
      service?.lookupAsync(SECP256K1.KeyPair.random().publicKey())?.thenAccept {
        for (newNode in it) {
          if (nodes.add(newNode)) {
            if (listeners != null) {
              for (listener in listeners) {
                listener(newNode)
              }
            }
          }
        }
      }
      delay(20 * 1000L)
    }
  }

  fun stop() = async {
    if (started.compareAndSet(true, false)) {
      service?.shutdown()
    }
  }
}
