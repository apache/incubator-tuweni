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
      DEFAULT_NETWORK
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
      DEFAULT_NETWORK
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
      DEFAULT_NETWORK
    )
  }
}
