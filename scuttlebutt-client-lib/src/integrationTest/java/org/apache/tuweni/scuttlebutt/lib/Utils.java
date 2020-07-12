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
import static org.apache.tuweni.scuttlebutt.lib.ScuttlebuttClientFactory.DEFAULT_NETWORK;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.io.Base64;
import org.apache.tuweni.scuttlebutt.Invite;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;

class Utils {

  private static Signature.KeyPair getLocalKeys() throws Exception {
    Map<String, String> env = System.getenv();

    Path secretPath = Paths.get(env.getOrDefault("ssb_dir", "ssb-keys"), "secret");
    File file = secretPath.toFile();

    if (!file.exists()) {
      throw new Exception("Secret file does not exist " + secretPath.toAbsolutePath());
    }

    Scanner s = new Scanner(file, UTF_8.name());
    s.useDelimiter("\n");

    ArrayList<String> list = new ArrayList<>();
    while (s.hasNext()) {
      String next = s.next();

      // Filter out the comment lines
      if (!next.startsWith("#")) {
        list.add(next);
      }
    }

    String secretJSON = String.join("", list);

    ObjectMapper mapper = new ObjectMapper();

    HashMap<String, String> values = mapper.readValue(secretJSON, new TypeReference<Map<String, String>>() {});
    String pubKey = values.get("public").replace(".ed25519", "");
    String privateKey = values.get("private").replace(".ed25519", "");

    Bytes pubKeyBytes = Base64.decode(pubKey);
    Bytes privKeyBytes = Base64.decode(privateKey);

    Signature.PublicKey pub = Signature.PublicKey.fromBytes(pubKeyBytes);
    Signature.SecretKey secretKey = Signature.SecretKey.fromBytes(privKeyBytes);

    return new Signature.KeyPair(pub, secretKey);
  }

  static ScuttlebuttClient getMasterClient(Vertx vertx) throws Exception {
    Map<String, String> env = System.getenv();
    String host = env.getOrDefault("ssb_host", "localhost");
    int port = Integer.parseInt(env.getOrDefault("ssb_port", "8008"));
    Signature.KeyPair serverKeypair = getLocalKeys();
    // log in as the server to have master rights.
    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult = ScuttlebuttClientFactory
        .fromNetWithNetworkKey(vertx, host, port, serverKeypair, serverKeypair.publicKey(), DEFAULT_NETWORK);

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
            DEFAULT_NETWORK);

    return scuttlebuttClientLibAsyncResult.get();
  }

  static ScuttlebuttClient connectWithInvite(Vertx vertx, Invite invite) throws Exception {
    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult =
        ScuttlebuttClientFactory.withInvite(vertx, Signature.KeyPair.random(), invite, DEFAULT_NETWORK);

    return scuttlebuttClientLibAsyncResult.get();
  }
}
