/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.lib;


import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.io.Base64;
import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient;
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler;

import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;

/**
 * A factory for constructing a new instance of ScuttlebuttClient with the given configuration parameters
 */
public final class ScuttlebuttClientFactory {

  private static final Bytes32 DEFAULT_NETWORK =
      Bytes32.wrap(Base64.decode("1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s="));

  private ScuttlebuttClientFactory() {}

  /**
   * Creates a scuttlebutt client by connecting with the given host, port and keypair
   *
   * @param mapper The ObjectMapper for serializing the content of published scuttlebutt messages
   * @param host The host to connect to as a scuttlebutt client
   * @param port The port to connect on
   * @param keyPair The keys to use for the secret handshake
   * @return the scuttlebutt client
   */
  public static AsyncResult<ScuttlebuttClient> fromNet(
      ObjectMapper mapper,
      String host,
      int port,
      Signature.KeyPair keyPair) {
    Vertx vertx = Vertx.vertx();
    return fromNetWithVertx(mapper, vertx, host, port, keyPair);
  }

  /**
   * Creates a scuttlebutt client by connecting with the given host, port and keypair using the given vertx instance.
   *
   * @param mapper The ObjectMapper for serializing the content of published scuttlebutt messages
   * @param vertx the vertx instance to use for network IO
   * @param host The host to connect to as a scuttlebutt client
   * @param port The port to connect on
   * @param keyPair The keys to use for the secret handshake
   * @return the scuttlebutt client
   */
  public static AsyncResult<ScuttlebuttClient> fromNetWithVertx(
      ObjectMapper mapper,
      Vertx vertx,
      String host,
      int port,
      Signature.KeyPair keyPair) {

    return fromNetWithNetworkKey(vertx, host, port, keyPair, DEFAULT_NETWORK, mapper);
  }

  /**
   * @param vertx the vertx instance to use for network IO
   * @param host The host to connect to as a scuttlebutt client
   * @param port The port to connect on
   * @param keyPair The keys to use for the secret handshake
   * @param networkIdentifier The scuttlebutt network key to use.
   * @param objectMapper The ObjectMapper for serializing the content of published scuttlebutt messages
   * @return the scuttlebutt client
   */
  public static AsyncResult<ScuttlebuttClient> fromNetWithNetworkKey(
      Vertx vertx,
      String host,
      int port,
      Signature.KeyPair keyPair,
      Bytes32 networkIdentifier,
      ObjectMapper objectMapper) {

    SecureScuttlebuttVertxClient secureScuttlebuttVertxClient =
        new SecureScuttlebuttVertxClient(vertx, keyPair, networkIdentifier);

    return secureScuttlebuttVertxClient
        .connectTo(
            port,
            host,
            keyPair.publicKey(),
            (
                Consumer<Bytes> sender,
                Runnable terminationFn) -> new RPCHandler(vertx, sender, terminationFn, objectMapper))
        .thenApply(handler -> new ScuttlebuttClient(handler, objectMapper));
  }

}
