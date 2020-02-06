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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
  private final static long DELAY = 5000;
  private final static long PING_PERIOD = 15000;
  private final static long REPORTING_PERIOD = 15000;

  private final Vertx vertx;
  private final URI ethstatsServerURI;
  private final Logger logger;
  private final AtomicBoolean started = new AtomicBoolean(false);
  private final AtomicBoolean waitingOnPong = new AtomicBoolean(false);
  private final NodeInfo nodeInfo;
  private final String secret;
  private WorkerExecutor executor;
  private HttpClient client;

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
      String osVer) {
    this.vertx = vertx;
    this.logger = logger;
    this.ethstatsServerURI = ethstatsServerURI;
    this.secret = secret;
    this.nodeInfo = new NodeInfo(name, node, port, network, protocol, os, osVer);
  }

  public void start() {
    if (started.compareAndSet(false, true)) {
      executor = vertx.createSharedWorkerExecutor("ethnetstats");
      client = vertx.createHttpClient(new HttpClientOptions().setLogActivity(true));
      startInternal();
    }
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

  public void stop() {
    if (started.compareAndSet(true, false)) {
      logger.debug("Stopping the service");
      executor.close();
    }
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

          writeCommand(ws, "hello", new AuthMessage(nodeInfo, secret));
        },
        e -> {
          result.fail(e);
        });
  }

  private void handleEmitEvent(ArrayNode event, WebSocket ws) {
    String command = event.get(0).textValue();
    switch (command) {
      case "node-pong":
        logger.debug("Received a pong {}", event.get(1));
        if (!waitingOnPong.compareAndSet(true, false)) {
          logger.warn("Received pong when we didn't expect one");
        } else {
          long start = event.get(1).get("start").longValue();
          long latency = (Instant.now().toEpochMilli() - start) / (2 * 1000);
          Map<String, Object> payload = new HashMap<>();
          payload.put("id", nodeInfo.getName());
          payload.put("latency", latency);
          writeCommand(ws, "latency", payload);
        }
        break;
      case "history":
        logger.debug("History request {}", event.get(1));
        break;
      default:
        logger.warn("Unexpected message {}", command);

    }
  }

  private void writePing(WebSocket ws) {
    Map<String, Object> payload = new HashMap<>();
    payload.put("id", nodeInfo.getName());
    payload.put("clientTime", Instant.now().toEpochMilli());
    writeCommand(ws, "node-ping", payload);
  }

  private void reportPeriodically(WebSocket ws) {
    TimeoutStream reportingStream = vertx.periodicStream(REPORTING_PERIOD).handler(ev -> {
      report(ws);
    });
    ws.closeHandler(h -> {
      reportingStream.cancel();
      attemptConnect(null);
    });
  }

  private void report(WebSocket ws) {
    writePing(ws);
    writeBlock(ws);
  }

  private void writeBlock(WebSocket ws) {
    Map<String, Object> details = new HashMap<>();
    details.put("id", nodeInfo.getName());
    details.put("block", assembleBlockStats());
    writeCommand(ws, "block", details);
  }

  private Object assembleBlockStats() {}


  private void writeCommand(WebSocket ws, String command, Object payload) {
    try {
      String message =
          mapper.writer().writeValueAsString(Collections.singletonMap("emit", Arrays.asList(command, payload)));
      logger.debug("Sending ping message {}", message);
      ws.writeTextMessage(message);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
