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

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.io.Base64;
import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient;
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import org.logl.Level;
import org.logl.LoggerProvider;
import org.logl.logl.SimpleLogger;

/**
 * A factory for constructing a new instance of ScuttlebuttClient with the given configuration parameters
 */
public final class ScuttlebuttClientFactory {

  public static Bytes32 DEFAULT_NETWORK = defaultNetwork();

  /**
   * @return the default scuttlebutt network key.
   */
  public static Bytes32 defaultNetwork() {
    // The network key for the main public network
    String networkKeyBase64 = "1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s=";
    return Bytes32.wrap(Base64.decode(networkKeyBase64));
  }

  private ScuttlebuttClientFactory() {}

  /**
   * Creates a scuttllebutt client by connecting with the given host, port and keypair
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
   * Creates a scuttllebutt client by connecting with the given host, port and keypair using the given vertx instance.
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
    LoggerProvider loggerProvider = SimpleLogger.withLogLevel(Level.DEBUG).toPrintWriter(
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8))));

    return fromNetWithNetworkKey(vertx, host, port, keyPair, DEFAULT_NETWORK, mapper, loggerProvider);
  }

  /**
   * @param vertx the vertx instance to use for network IO
   * @param host The host to connect to as a scuttlebutt client
   * @param port The port to connect on
   * @param keyPair The keys to use for the secret handshake
   * @param networkIdentifier The scuttlebutt network key to use.
   * @param objectMapper The ObjectMapper for serializing the content of published scuttlebutt messages
   * @param loggerProvider the logging configuration provider
   * @return the scuttlebutt client
   */
  public static AsyncResult<ScuttlebuttClient> fromNetWithNetworkKey(
      Vertx vertx,
      String host,
      int port,
      Signature.KeyPair keyPair,
      Bytes32 networkIdentifier,
      ObjectMapper objectMapper,
      LoggerProvider loggerProvider) {

    SecureScuttlebuttVertxClient secureScuttlebuttVertxClient =
        new SecureScuttlebuttVertxClient(loggerProvider, vertx, keyPair, networkIdentifier);

    return secureScuttlebuttVertxClient
        .connectTo(
            port,
            host,
            keyPair.publicKey(),
            (
                Consumer<Bytes> sender,
                Runnable terminationFn) -> new RPCHandler(vertx, sender, terminationFn, objectMapper, loggerProvider))
        .thenApply(handler -> new ScuttlebuttClient(handler, objectMapper));
  }

}
