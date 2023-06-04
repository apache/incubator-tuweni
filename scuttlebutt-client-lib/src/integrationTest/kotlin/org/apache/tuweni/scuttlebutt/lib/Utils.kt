// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib

import io.vertx.core.Vertx
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.lib.KeyFileLoader.getLocalKeys
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClientFactory.DEFAULT_NETWORK
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClientFactory.fromNetWithNetworkKey
import org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClientFactory.withInvite
import java.nio.file.Paths

internal object Utils {
  @get:Throws(Exception::class)
  private val localKeys: Signature.KeyPair
    get() {
      val ssbPath = Paths.get(System.getenv().getOrDefault("ssb_dir", "/tmp/ssb"))
      return getLocalKeys(ssbPath)
    }

  @Throws(Exception::class)
  fun getMasterClient(vertx: Vertx): ScuttlebuttClient {
    val env = System.getenv()
    val host = env.getOrDefault("ssb_host", "localhost")
    val port = env.getOrDefault("ssb_port", "8008").toInt()
    val serverKeypair = localKeys
    // log in as the server to have master rights.
    return fromNetWithNetworkKey(
      vertx,
      host,
      port,
      serverKeypair,
      serverKeypair.publicKey(),
      DEFAULT_NETWORK,
    )
  }

  @Throws(Exception::class)
  fun getNewClient(vertx: Vertx): ScuttlebuttClient {
    val env = System.getenv()
    val host = env.getOrDefault("ssb_host", "localhost")
    val port = env.getOrDefault("ssb_port", "8008").toInt()
    val serverKeypair = localKeys
    return fromNetWithNetworkKey(
      vertx,
      host,
      port,
      Signature.KeyPair.random(),
      serverKeypair.publicKey(),
      DEFAULT_NETWORK,
    )
  }

  @Throws(Exception::class)
  fun connectWithInvite(vertx: Vertx, invite: Invite): ScuttlebuttClient {
    val env = System.getenv()
    val actualHost = env.getOrDefault("ssb_host", "localhost")
    val port = env.getOrDefault("ssb_port", "8008").toInt()
    val recalibratedInvite = Invite(actualHost, port, invite.identity, invite.seedKey)
    return withInvite(
      vertx,
      Signature.KeyPair.random(),
      recalibratedInvite,
      DEFAULT_NETWORK,
    )
  }
}
