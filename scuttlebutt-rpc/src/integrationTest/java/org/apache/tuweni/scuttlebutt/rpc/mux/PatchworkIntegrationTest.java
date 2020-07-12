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
package org.apache.tuweni.scuttlebutt.rpc.mux;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.io.Base64;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient;
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest;
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction;
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse;
import org.apache.tuweni.scuttlebutt.rpc.RPCStreamRequest;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletionException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class PatchworkIntegrationTest {

  @Test
  public void testWithPatchwork(@VertxInstance Vertx vertx) throws Exception {

    RPCHandler rpcHandler = makeRPCHandler(vertx);

    List<AsyncResult<RPCResponse>> results = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
      RPCFunction function = new RPCFunction("whoami");
      RPCAsyncRequest asyncRequest = new RPCAsyncRequest(function, new ArrayList<>());

      AsyncResult<RPCResponse> res = rpcHandler.makeAsyncRequest(asyncRequest);

      results.add(res);
    }

    AsyncResult<List<RPCResponse>> allResults = AsyncResult.combine(results);
    List<RPCResponse> rpcMessages = allResults.get();

    assertEquals(10, rpcMessages.size());
  }


  // TODO: Move this to a utility class that all the scuttlebutt modules' tests can use.
  private static Signature.KeyPair getLocalKeys() throws Exception {
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

    return new Signature.KeyPair(pub, secretKey);
  }

  @Test
  void postMessageTest(@VertxInstance Vertx vertx) throws Exception {

    RPCHandler rpcHandler = makeRPCHandler(vertx);


    List<AsyncResult<RPCResponse>> results = new ArrayList<>();

    for (int i = 0; i < 20; i++) {
      // Note: in a real use case, this would more likely be a Java class with these fields
      HashMap<String, String> params = new HashMap<>();
      params.put("type", "post");
      params.put("text", "test test " + i);

      RPCAsyncRequest asyncRequest = new RPCAsyncRequest(new RPCFunction("publish"), Arrays.asList(params));

      AsyncResult<RPCResponse> rpcMessageAsyncResult = rpcHandler.makeAsyncRequest(asyncRequest);

      results.add(rpcMessageAsyncResult);

    }

    List<RPCResponse> rpcMessages = AsyncResult.combine(results).get();

    rpcMessages.forEach(msg -> System.out.println(msg.asString()));
  }

  @Test
  /**
   * We expect this to complete the AsyncResult with an exception.
   */
  public void postMessageThatIsTooLong(@VertxInstance Vertx vertx) throws Exception {

    RPCHandler rpcHandler = makeRPCHandler(vertx);

    List<AsyncResult<RPCResponse>> results = new ArrayList<>();

    String longString = new String(new char[40000]).replace("\0", "a");

    for (int i = 0; i < 20; i++) {
      // Note: in a real use case, this would more likely be a Java class with these fields
      HashMap<String, String> params = new HashMap<>();
      params.put("type", "post");
      params.put("text", longString);

      RPCAsyncRequest asyncRequest = new RPCAsyncRequest(new RPCFunction("publish"), Arrays.asList(params));

      AsyncResult<RPCResponse> rpcMessageAsyncResult = rpcHandler.makeAsyncRequest(asyncRequest);

      results.add(rpcMessageAsyncResult);

    }

    CompletionException exception = assertThrows(CompletionException.class, () -> {
      AsyncResult.combine(results).get();
    });
    assertEquals("encoded message must not be larger than 8192 bytes", exception.getCause().getMessage());
  }

  private RPCHandler makeRPCHandler(Vertx vertx) throws Exception {
    Signature.KeyPair keyPair = getLocalKeys();
    String networkKeyBase64 = "1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s=";
    Bytes32 networkKeyBytes32 = Bytes32.wrap(Base64.decode(networkKeyBase64));

    String host = "localhost";
    int port = 8008;

    SecureScuttlebuttVertxClient secureScuttlebuttVertxClient =
        new SecureScuttlebuttVertxClient(vertx, keyPair, networkKeyBytes32);

    AsyncResult<RPCHandler> onConnect = secureScuttlebuttVertxClient
        .connectTo(
            port,
            host,
            keyPair.publicKey(),
            (sender, terminationFn) -> new RPCHandler(vertx, sender, terminationFn));

    return onConnect.get();
  }


  @Test
  void streamTest(@VertxInstance Vertx vertx) throws Exception {

    RPCHandler handler = makeRPCHandler(vertx);
    Signature.PublicKey publicKey = getLocalKeys().publicKey();

    String pubKey = "@" + Base64.encode(publicKey.bytes()) + ".ed25519";

    Map<String, String> params = new HashMap<>();
    params.put("id", pubKey);

    CompletableAsyncResult<Void> streamEnded = AsyncResult.incomplete();

    RPCStreamRequest streamRequest = new RPCStreamRequest(new RPCFunction("createUserStream"), Arrays.asList(params));

    handler.openStream(streamRequest, (closeStream) -> new ScuttlebuttStreamHandler() {
      @Override
      public void onMessage(RPCResponse message) {
        System.out.print(message.asString());
      }

      @Override
      public void onStreamEnd() {
        streamEnded.complete(null);
      }

      @Override
      public void onStreamError(Exception ex) {

        streamEnded.completeExceptionally(ex);
      }
    });

    // Wait until the stream is complete
    streamEnded.get();

  }


}
