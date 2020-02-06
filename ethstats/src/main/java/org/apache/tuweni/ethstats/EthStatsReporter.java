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
package org.apache.tuweni.ethstats;

import org.apache.tuweni.eth.EthJsonModule;
import org.apache.tuweni.units.bigints.UInt256;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.TimeoutStream;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import org.logl.Logger;

/**
 * ETHNetStats reporting service.
 * <p>
 * This service connects to a running ethnetstats service and reports.
 * <p>
 * If the service is not available, the reporter will keep trying to connect periodically. The service will report
 * statistics over time.
 */
public final class EthStatsReporter {

  private final static ObjectMapper mapper = new ObjectMapper();

  static {
    mapper.registerModule(new EthJsonModule());
  }
  private final static long DELAY = 5000;
  private final static long REPORTING_PERIOD = 1000;
  private final static long PING_PERIOD = 15000;


  private final String id;
  private final Vertx vertx;
  private final URI ethstatsServerURI;
  private final Logger logger;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean waitingOnPong = new AtomicBoolean(false);
  private final NodeInfo nodeInfo;
  private final String secret;
  private final AtomicReference<Integer> newTxCount = new AtomicReference<>();
  private final Consumer<List<UInt256>> historyRequester;

  private WorkerExecutor executor;
  private HttpClient client;
  private AtomicReference<BlockStats> newHead = new AtomicReference<>();
  private AtomicReference<NodeStats> newNodeStats = new AtomicReference<>();
  private AtomicReference<List<BlockStats>> newHistory = new AtomicReference<>();

  /**
   * Default constructor.
   * 
   * @param vertx a Vert.x instance, externally managed.
   * @param logger a logger
   * @param ethstatsServerURI the URI to connect to eth-netstats, such as ws://www.ethnetstats.org:3000/api
   * @param secret the secret to use when we connect to eth-netstats
   * @param name the name of the node to be reported in the UI
   * @param node the node name to be reported in the UI
   * @param port the devp2p port exposed by this node
   * @param network the network id
   * @param protocol the version of the devp2p eth subprotocol, such as eth/63
   * @param os the operating system on which the node runs
   * @param osVer the version of the OS on which the node runs
   * @param historyRequester a hook for ethstats to request block information by number.
   */
  public EthStatsReporter(
      Vertx vertx,
      Logger logger,
      URI ethstatsServerURI,
      String secret,
      String name,
      String node,
      int port,
      String network,
      String protocol,
      String os,
      String osVer,
      Consumer<List<UInt256>> historyRequester) {
    this.id = UUID.randomUUID().toString();
    this.vertx = vertx;
    this.logger = logger;
    this.ethstatsServerURI = ethstatsServerURI;
    this.secret = secret;
    this.nodeInfo = new NodeInfo(name, node, port, network, protocol, os, osVer);
    this.historyRequester = historyRequester;
  }

  public void start() {
    if (started.compareAndSet(false, true)) {
      executor = vertx.createSharedWorkerExecutor("ethnetstats");
      client = vertx.createHttpClient(new HttpClientOptions().setLogActivity(true));
      startInternal();
    }
  }

  public void stop() {
    if (started.compareAndSet(true, false)) {
      logger.debug("Stopping the service");
      executor.close();
    }
  }

  public void sendNewHead(BlockStats newBlockStats) {
    newHead.set(newBlockStats);
  }

  public void sendNewPendingTransactionCount(int txCount) {
    newTxCount.set(txCount);
  }

  public void sendNewNodeStats(NodeStats nodeStats) {
    newNodeStats.set(nodeStats);
  }

  public void sendHistoryResponse(List<BlockStats> blocks) {
    newHistory.set(blocks);
  }

  private void startInternal() {
    executor.executeBlocking(this::connect, result -> {
      if (started.get()) {
        if ((result.failed() || !result.result())) {
          logger.debug("Attempting to connect", result.cause());
          attemptConnect(null);
        }
      }
    });
  }

  private void attemptConnect(Void aVoid) {
    vertx.setTimer(DELAY, handler -> this.startInternal());
  }

  private void connect(Future<Boolean> result) {
    client.websocket(
        ethstatsServerURI.getPort(),
        ethstatsServerURI.getHost(),
        ethstatsServerURI.toString(),
        MultiMap.caseInsensitiveMultiMap().add("origin", "http://localhost"),
        ws -> {
          ws.closeHandler(this::attemptConnect);
          ws.exceptionHandler(e -> {
            logger.debug("Error while communicating with ethnetstats", e);

          });
          ws.textMessageHandler(message -> {
            try {
              JsonNode node = mapper.readTree(message);
              JsonNode emitEvent = node.get("emit");
              if (emitEvent.isArray()) {
                String eventValue = emitEvent.get(0).textValue();
                if (!result.isComplete()) {
                  if (!"ready".equals(eventValue)) {
                    logger.warn(message);
                    result.complete(false);
                  } else {
                    logger.debug("Connected OK! {}", message);
                    result.complete(true);

                    // we are connected and now sending information
                    reportPeriodically(ws);
                    writePing(ws);
                    report(ws);
                  }
                } else {
                  handleEmitEvent((ArrayNode) emitEvent, ws);
                }
              } else {
                logger.warn(message);
                result.complete(false);
              }
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });

          writeCommand(ws, "hello", new AuthMessage(nodeInfo, id, secret));
        },
        result::fail);
  }

  private void handleEmitEvent(ArrayNode event, WebSocket ws) {
    String command = event.get(0).textValue();
    switch (command) {
      case "node-pong":
        logger.debug("Received a pong {}", event.get(1));
        if (!waitingOnPong.compareAndSet(true, false)) {
          logger.warn("Received pong when we didn't expect one");
        } else {
          long start = event.get(1).get("clientTime").longValue();
          long latency = (Instant.now().toEpochMilli() - start) / (2 * 1000);
          writeCommand(ws, "latency", "latency", latency);
        }
        break;
      case "history":
        logger.debug("History request {}", event.get(1));
        requestHistory(event.get(1));
        break;
      default:
        logger.warn("Unexpected message {}", command);

    }
  }

  private void requestHistory(JsonNode list) {
    historyRequester.accept(null);
  }

  private void writePing(WebSocket ws) {
    waitingOnPong.set(true);
    writeCommand(ws, "node-ping", "clientTime", Instant.now().toEpochMilli());
  }

  private void reportPeriodically(WebSocket ws) {
    TimeoutStream reportingStream = vertx.periodicStream(REPORTING_PERIOD).handler(ev -> {
      report(ws);
    });
    TimeoutStream pingStream = vertx.periodicStream(PING_PERIOD).handler(ev -> {
      writePing(ws);
    });
    ws.closeHandler(h -> {
      reportingStream.cancel();
      pingStream.cancel();
      attemptConnect(null);
    });
  }

  private void report(WebSocket ws) {
    BlockStats head = newHead.getAndSet(null);
    if (head != null) {
      writeCommand(ws, "block", "block", head);
    }
    Integer count = newTxCount.getAndSet(null);
    if (count != null) {
      writeCommand(ws, "pending", "stats", Collections.singletonMap("pending", count));
    }
    NodeStats nodeStats = newNodeStats.getAndSet(null);
    if (nodeStats != null) {
      writeCommand(ws, "stats", "stats", nodeStats);
    }
    List<BlockStats> newBlocks = newHistory.getAndSet(null);
    if (newBlocks != null && !newBlocks.isEmpty()) {
      writeCommand(ws, "history", "history", newBlocks);
    }
  }

  private void writeCommand(WebSocket ws, String command, Object payload) {
    try {
      String message =
          mapper.writer().writeValueAsString(Collections.singletonMap("emit", Arrays.asList(command, payload)));
      logger.debug("Sending {} message {}", command, message);
      ws.writeTextMessage(message);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void writeCommand(WebSocket ws, String command, String key, Object payload) {
    Map<String, Object> body = new HashMap<>();
    body.put("id", id);
    body.put(key, payload);
    writeCommand(ws, command, body);
  }
}
