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
package org.apache.tuweni.scuttlebutt.rpc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.io.Base64;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.scuttlebutt.handshake.vertx.ClientHandler;
import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient;

import java.io.BufferedWriter;
import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logl.Level;
import org.logl.LoggerProvider;
import org.logl.logl.SimpleLogger;
import org.logl.vertx.LoglLogDelegateFactory;

/**
 * Test used with a local installation of Patchwork on the developer machine.
 * <p>
 * Usable as a demo or to check manually connections.
 */
@ExtendWith(VertxExtension.class)
class PatchworkIntegrationTest {

  public static class MyClientHandler implements ClientHandler {

    private final Consumer<Bytes> sender;
    private final Runnable terminationFn;

    public MyClientHandler(Consumer<Bytes> sender, Runnable terminationFn) {
      this.sender = sender;
      this.terminationFn = terminationFn;
    }

    @Override
    public void receivedMessage(Bytes message) {

      RPCMessage rpcMessage = new RPCMessage(message);

      System.out.println(rpcMessage.asString());


    }

    @Override
    public void streamClosed() {

      System.out.println("Stream closed?");

    }

    void sendMessage(Bytes bytes) {
      System.out.println("Sending message?");
      sender.accept(bytes);
    }

    void closeStream() {
      terminationFn.run();
    }
  }

  @Disabled
  @Test
  void runWithPatchWork(@VertxInstance Vertx vertx) throws Exception {
    String host = "localhost";
    int port = 8008;
    LoggerProvider loggerProvider = SimpleLogger.withLogLevel(Level.DEBUG).toPrintWriter(
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8))));
    LoglLogDelegateFactory.setProvider(loggerProvider);

    Optional<String> ssbDir = Optional.fromNullable(System.getenv().get("ssb_dir"));
    Optional<String> homePath =
        Optional.fromNullable(System.getProperty("user.home")).transform(home -> home + "/.ssb");

    Optional<String> path = ssbDir.or(homePath);

    if (!path.isPresent()) {
      throw new Exception("Cannot find ssb directory config value");
    }

    String secretPath = path.get() + "/secret";
    File file = new File(secretPath);

    if (!file.exists()) {
      throw new Exception("Secret file does not exist");
    }

    Scanner s = new Scanner(file, UTF_8.name());
    s.useDelimiter("\n");

    ArrayList<String> list = new ArrayList<String>();
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

    Signature.KeyPair keyPair = new Signature.KeyPair(pub, secretKey);
    String networkKeyBase64 = "1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s=";

    String serverPublicKey = pubKey; // TODO use your own identity public key here.
    Signature.PublicKey publicKey = Signature.PublicKey.fromBytes(Base64.decode(serverPublicKey));

    Bytes32 networkKeyBytes32 = Bytes32.wrap(Base64.decode(networkKeyBase64));

    SecureScuttlebuttVertxClient secureScuttlebuttVertxClient =
        new SecureScuttlebuttVertxClient(loggerProvider, vertx, keyPair, networkKeyBytes32);

    AsyncResult<ClientHandler> onConnect =
        secureScuttlebuttVertxClient.connectTo(port, host, publicKey, MyClientHandler::new);

    MyClientHandler clientHandler = (MyClientHandler) onConnect.get();
    assertTrue(onConnect.isDone());
    assertFalse(onConnect.isCompletedExceptionally());
    Thread.sleep(1000);
    assertNotNull(clientHandler);
    // An RPC command that just tells us our public key (like ssb-server whoami on the command line.)
    String rpcRequestBody = "{\"name\": [\"whoami\"],\"type\": \"async\",\"args\":[]}";
    Bytes rpcRequest = RPCCodec.encodeRequest(rpcRequestBody, RPCFlag.BodyType.JSON);

    System.out.println("Attempting RPC request...");
    clientHandler.sendMessage(rpcRequest);
    for (int i = 0; i < 10; i++) {
      clientHandler.sendMessage(RPCCodec.encodeRequest(rpcRequestBody, RPCFlag.BodyType.JSON));
    }

    Thread.sleep(10000);

    secureScuttlebuttVertxClient.stop().join();
  }
}
