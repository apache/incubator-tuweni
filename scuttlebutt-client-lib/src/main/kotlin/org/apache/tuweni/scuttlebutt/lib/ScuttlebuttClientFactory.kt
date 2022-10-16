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
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.io.Base64
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler

/**
 * A factory for constructing a new instance of ScuttlebuttClient with the given configuration parameters
 */
object ScuttlebuttClientFactory {
  @JvmStatic
  val DEFAULT_NETWORK = Bytes32.wrap(Base64.decode("1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s="))

  /**
   * Creates a scuttlebutt client by connecting with the given host, port and keypair using the given vertx instance.
   *
   * @param vertx the vertx instance to use for network IO
   * @param host The host to connect to as a scuttlebutt client
   * @param port The port to connect on
   * @param keyPair The keys to use for the secret handshake
   * @param serverPublicKey the public key of the server we connect to
   * @return the scuttlebutt client
   */
  @JvmStatic
  fun fromNetWithVertx(
    vertx: Vertx,
    host: String,
    port: Int,
    keyPair: Signature.KeyPair,
    serverPublicKey: Signature.PublicKey
  ): ScuttlebuttClient {
    return fromNetWithNetworkKey(vertx, host, port, keyPair, serverPublicKey, DEFAULT_NETWORK)
  }

  /**
   * Creates a SSB client with a network key
   *
   * @param vertx the vertx instance to use for network IO
   * @param host The host to connect to as a scuttlebutt client
   * @param port The port to connect on
   * @param keyPair The keys to use for the secret handshake
   * @param serverPublicKey the public key of the server we connect to
   * @param networkIdentifier The scuttlebutt network key to use.
   * @return the scuttlebutt client
   */
  @JvmStatic
  fun fromNetWithNetworkKey(
    vertx: Vertx,
    host: String,
    port: Int,
    keyPair: Signature.KeyPair?,
    serverPublicKey: Signature.PublicKey?,
    networkIdentifier: Bytes32?
  ): ScuttlebuttClient {
    val secureScuttlebuttVertxClient = SecureScuttlebuttVertxClient(
      vertx,
      keyPair!!,
      networkIdentifier!!
    )
    return runBlocking {
      val client = secureScuttlebuttVertxClient.connectTo(
        port,
        host,
        serverPublicKey,
        null
      ) { sender, terminationFn ->
        RPCHandler(
          vertx,
          sender,
          terminationFn
        )
      } as RPCHandler
      return@runBlocking ScuttlebuttClient(client)
    }
  }

  /**
   * Creates a SSB client with an invite
   *
   * @param vertx the vertx instance to use for network IO
   * @param keyPair The keys to use for the secret handshake
   * @param invite the invitation to the remote server
   * @param networkIdentifier The scuttlebutt network key to use.
   * @return the scuttlebutt client
   */
  @JvmStatic
  fun withInvite(
    vertx: Vertx,
    keyPair: Signature.KeyPair,
    invite: Invite,
    networkIdentifier: Bytes32
  ): ScuttlebuttClient {
    val secureScuttlebuttVertxClient = SecureScuttlebuttVertxClient(
      vertx,
      keyPair,
      networkIdentifier
    )
    return runBlocking {
      val multiplexer: RPCHandler = secureScuttlebuttVertxClient
        .connectTo(
          invite.port,
          invite.host,
          null,
          invite
        ) { sender, terminationFn ->
          RPCHandler(
            vertx,
            sender,
            terminationFn
          )
        } as RPCHandler
      return@runBlocking ScuttlebuttClient(multiplexer)
    }
  }
}
