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

import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.scuttlebutt.Invite;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import io.vertx.core.Vertx;

class Utils {

  private static Signature.KeyPair getLocalKeys() throws Exception {
    Path ssbPath = Paths.get(System.getenv().getOrDefault("ssb_dir", "/tmp/ssb"));

    return KeyFileLoader.getLocalKeys(ssbPath);
  }

  static ScuttlebuttClient getMasterClient(Vertx vertx) throws Exception {
    Map<String, String> env = System.getenv();
    String host = env.getOrDefault("ssb_host", "localhost");
    int port = Integer.parseInt(env.getOrDefault("ssb_port", "8008"));
    Signature.KeyPair serverKeypair = getLocalKeys();
    // log in as the server to have master rights.
    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult = ScuttlebuttClientFactory
        .fromNetWithNetworkKey(
            vertx,
            host,
            port,
            serverKeypair,
            serverKeypair.publicKey(),
            ScuttlebuttClientFactory.getDEFAULT_NETWORK());

    return scuttlebuttClientLibAsyncResult.get();
  }

  static ScuttlebuttClient getNewClient(Vertx vertx) throws Exception {
    Map<String, String> env = System.getenv();
    String host = env.getOrDefault("ssb_host", "localhost");
    int port = Integer.parseInt(env.getOrDefault("ssb_port", "8008"));
    Signature.KeyPair serverKeypair = getLocalKeys();
    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult = ScuttlebuttClientFactory
        .fromNetWithNetworkKey(
            vertx,
            host,
            port,
            Signature.KeyPair.random(),
            serverKeypair.publicKey(),
            ScuttlebuttClientFactory.getDEFAULT_NETWORK());

    return scuttlebuttClientLibAsyncResult.get();
  }

  static ScuttlebuttClient connectWithInvite(Vertx vertx, Invite invite) throws Exception {
    Map<String, String> env = System.getenv();
    String actualHost = env.getOrDefault("ssb_host", "localhost");
    int port = Integer.parseInt(env.getOrDefault("ssb_port", "8008"));
    Invite recalibratedInvite = new Invite(actualHost, port, invite.identity(), invite.seedKey());
    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult = ScuttlebuttClientFactory
        .withInvite(
            vertx,
            Signature.KeyPair.random(),
            recalibratedInvite,
            ScuttlebuttClientFactory.getDEFAULT_NETWORK());

    return scuttlebuttClientLibAsyncResult.get();
  }
}
