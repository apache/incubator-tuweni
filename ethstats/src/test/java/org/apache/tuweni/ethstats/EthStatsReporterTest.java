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

import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;

import java.net.URI;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logl.Level;
import org.logl.Logger;
import org.logl.logl.SimpleLogger;

@ExtendWith(VertxExtension.class)
public class EthStatsReporterTest {


  @Test
  void testConnectToLocalEthStats(@VertxInstance Vertx vertx) throws InterruptedException {
    Logger logger = SimpleLogger.withLogLevel(Level.DEBUG).toOutputStream(System.out).getLogger("wat");

    EthStatsReporter reporter = new EthStatsReporter(
        vertx,
        logger,
        URI.create("ws://localhost:3000/api"),
        "wat",
        "name",
        "node",
        33030,
        "10",
        "eth/63",
        "Windoz",
        "64");

    reporter.start();

    Thread.sleep(30 * 1000);

    reporter.stop();
  }
}
