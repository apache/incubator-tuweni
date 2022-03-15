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
package org.apache.tuweni.gossip;


import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncCompletion;
import org.apache.tuweni.concurrent.CompletableAsyncCompletion;
import org.apache.tuweni.crypto.Hash;
import org.apache.tuweni.plumtree.vertx.VertxGossipServer;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.Security;
import java.time.Instant;
import java.util.Collections;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

/**
 * Application running a gossip client, taking configuration from command line or a configuration file.
 *
 */
public final class GossipApp {

  private static final Logger logger = LoggerFactory.getLogger(GossipApp.class.getName());

  public static void main(String[] args) {
    Security.addProvider(new BouncyCastleProvider());
    GossipCommandLineOptions opts = CommandLine.populateCommand(new GossipCommandLineOptions(), args);
    try {
      opts.validate();
    } catch (IllegalArgumentException e) {
      logger.error("Invalid configuration detected.\n\n{}", e.getMessage());
      new CommandLine(opts).usage(System.out);
      System.exit(1);
    }
    if (opts.help()) {
      new CommandLine(opts).usage(System.out);
      System.exit(0);
    }
    GossipApp gossipApp = new GossipApp(Vertx.vertx(), opts, System.err, System.out, () -> System.exit(1));
    Runtime.getRuntime().addShutdownHook(new Thread(gossipApp::stop));
    gossipApp.start();
  }

  private final ExecutorService senderThreadPool = Executors.newSingleThreadExecutor(r -> {
    Thread t = new Thread(r, "sender");
    t.setDaemon(false);
    return t;
  });
  private final GossipCommandLineOptions opts;
  private final Runnable terminateFunction;
  private final PrintStream errStream;
  private final PrintStream outStream;
  private final VertxGossipServer server;
  private final HttpServer rpcServer;
  private final ExecutorService fileWriter = Executors.newSingleThreadExecutor();

  GossipApp(
      Vertx vertx,
      GossipCommandLineOptions opts,
      PrintStream errStream,
      PrintStream outStream,
      Runnable terminateFunction) {
    LoggingPeerRepository repository = new LoggingPeerRepository();
    logger.info("Setting up server on {}:{}", opts.networkInterface(), opts.listenPort());
    server = new VertxGossipServer(
        vertx,
        opts.networkInterface(),
        opts.listenPort(),
        Hash::keccak256,
        repository,
        (bytes, attr, peer) -> readMessage(opts.messageLog(), errStream, bytes),
        null,
        new CountingPeerPruningFunction(10),
        100,
        100);
    this.opts = opts;
    this.errStream = errStream;
    this.outStream = outStream;
    this.terminateFunction = terminateFunction;
    this.rpcServer = vertx.createHttpServer();
  }

  void start() {
    logger.info("Starting gossip");
    AsyncCompletion completion = server.start();
    try {
      completion.join();
    } catch (CompletionException | InterruptedException e) {
      logger.error("Server could not start: {}", e.getMessage());
      terminateFunction.run();
    }
    logger.info("TCP server started");

    CompletableAsyncCompletion rpcCompletion = AsyncCompletion.incomplete();
    rpcServer.requestHandler(this::handleRPCRequest).listen(opts.rpcPort(), opts.networkInterface(), res -> {
      if (res.failed()) {
        rpcCompletion.completeExceptionally(res.cause());
      } else {
        rpcCompletion.complete();
      }
    });
    try {
      rpcCompletion.join();
    } catch (CompletionException | InterruptedException e) {
      logger.error("RPC server could not start: " + e.getMessage());
      terminateFunction.run();
    }
    logger.info("RPC server started");

    try {
      AsyncCompletion
          .allOf(opts.peers().stream().map(peer -> server.connectTo(peer.getHost(), peer.getPort())))
          .join(60, TimeUnit.SECONDS);
    } catch (TimeoutException | InterruptedException e) {
      errStream.println("Server could not connect to other peers: " + e.getMessage());
    }
    logger.info("Gossip started");

    if (opts.sending()) {
      logger.info("Start sending messages");
      senderThreadPool.submit(() -> {
        for (int i = 0; i < opts.numberOfMessages(); i++) {
          if (Thread.currentThread().isInterrupted()) {
            return;
          }
          Bytes payload = Bytes.random(opts.payloadSize());
          publish(payload);
          try {
            Thread.sleep(opts.sendInterval());
          } catch (InterruptedException e) {
            return;
          }
        }
      });
    }
  }

  private void handleRPCRequest(HttpServerRequest httpServerRequest) {
    if (HttpMethod.POST.equals(httpServerRequest.method())) {
      if ("/publish".equals(httpServerRequest.path())) {
        httpServerRequest.bodyHandler(body -> {
          Bytes message = Bytes.wrapBuffer(body);
          outStream.println("Message to publish " + message.toHexString());
          publish(message);
          httpServerRequest.response().setStatusCode(200).end();
        });
      } else {
        httpServerRequest.response().setStatusCode(404).end();
      }
    } else {
      httpServerRequest.response().setStatusCode(405).end();
    }
  }

  void stop() {
    logger.info("Stopping sending");
    senderThreadPool.shutdown();
    logger.info("Stopping gossip");
    try {
      server.stop().join();
    } catch (InterruptedException e) {
      logger.error("Server could not stop: {}", e.getMessage());
      terminateFunction.run();
    }

    CompletableAsyncCompletion rpcCompletion = AsyncCompletion.incomplete();
    rpcServer.close(res -> {
      if (res.failed()) {
        rpcCompletion.completeExceptionally(res.cause());
      } else {
        rpcCompletion.complete();
      }
    });
    try {
      rpcCompletion.join();
    } catch (CompletionException | InterruptedException e) {
      logger.info("Stopped gossip");
      logger.error("RPC server could not stop: {}", e.getMessage());
      terminateFunction.run();
    }

    fileWriter.shutdown();
  }

  private void readMessage(String messageLog, PrintStream err, Bytes bytes) {
    fileWriter.submit(() -> {
      ObjectMapper mapper = new ObjectMapper();
      ObjectNode node = mapper.createObjectNode();
      node.put("timestamp", Instant.now().toString());
      node.put("value", bytes.toHexString());
      try {
        Path path = Paths.get(messageLog);
        Files
            .write(
                path,
                Collections.singletonList(mapper.writeValueAsString(node)),
                StandardCharsets.UTF_8,
                Files.exists(path) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);
      } catch (IOException e) {
        err.println(e.getMessage());
      }
    });
  }

  public void publish(Bytes message) {
    server.gossip("", message);
  }
}
