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

import static org.apache.tuweni.scuttlebutt.rpc.Utils.getLocalKeys;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.io.Base64;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.scuttlebutt.handshake.vertx.ClientHandler;
import org.apache.tuweni.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient;
import org.apache.tuweni.scuttlebutt.rpc.mux.RPCHandler;
import org.apache.tuweni.scuttlebutt.rpc.mux.ScuttlebuttStreamHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test used against a Securescuttlebutt server.
 *
 * The test requires the ssb_dir, ssb_host and ssb_port to be set.
 */
@ExtendWith(VertxExtension.class)
class RPCIntegrationTest {

  public static class MyClientHandler implements ClientHandler {

    private final Consumer<Bytes> sender;
    private final Runnable terminationFn;

    public MyClientHandler(Consumer<Bytes> sender, Runnable terminationFn) {
      this.sender = sender;
      this.terminationFn = terminationFn;
    }

    @Override
    public void receivedMessage(Bytes message) {
      new RPCMessage(message);
    }

    @Override
    public void streamClosed() {}

    void sendMessage(Bytes bytes) {
      sender.accept(bytes);
    }
  }

  /**
   * This test tests the connection to a local patchwork installation. You need to run patchwork locally to perform that
   * work.
   *
   */
  @Test
  void runWithPatchWork(@VertxInstance Vertx vertx) throws Exception {
    Map<String, String> env = System.getenv();

    String host = env.getOrDefault("ssb_host", "localhost");
    int port = Integer.parseInt(env.getOrDefault("ssb_port", "8008"));

    Signature.KeyPair keyPair = getLocalKeys();

    String networkKeyBase64 = "1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s=";

    Signature.PublicKey publicKey = keyPair.publicKey();

    Bytes32 networkKeyBytes32 = Bytes32.wrap(Base64.decode(networkKeyBase64));

    SecureScuttlebuttVertxClient secureScuttlebuttVertxClient =
        new SecureScuttlebuttVertxClient(vertx, keyPair, networkKeyBytes32);

    AsyncResult<ClientHandler> onConnect =
        secureScuttlebuttVertxClient.connectTo(port, host, publicKey, null, MyClientHandler::new);

    MyClientHandler clientHandler = (MyClientHandler) onConnect.get();
    assertTrue(onConnect.isDone());
    assertFalse(onConnect.isCompletedExceptionally());
    Thread.sleep(1000);
    assertNotNull(clientHandler);
    // An RPC command that just tells us our public key (like ssb-server whoami on the command line.)
    String rpcRequestBody = "{\"name\": [\"whoami\"],\"type\": \"async\",\"args\":[]}";
    Bytes rpcRequest = RPCCodec.encodeRequest(rpcRequestBody, RPCFlag.BodyType.JSON);

    clientHandler.sendMessage(rpcRequest);
    for (int i = 0; i < 10; i++) {
      clientHandler.sendMessage(RPCCodec.encodeRequest(rpcRequestBody, RPCFlag.BodyType.JSON));
    }

    Thread.sleep(10000);

    secureScuttlebuttVertxClient.stop().join();
  }

  @Test
  void testWithPatchwork(@VertxInstance Vertx vertx) throws Exception {

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


  /**
   * We expect this to complete the AsyncResult with an exception.
   */
  @Test
  void postMessageThatIsTooLong(@VertxInstance Vertx vertx) throws Exception {

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

    Map<String, String> env = System.getenv();
    String host = env.getOrDefault("ssb_host", "localhost");
    int port = Integer.parseInt(env.getOrDefault("ssb_port", "8008"));

    SecureScuttlebuttVertxClient secureScuttlebuttVertxClient =
        new SecureScuttlebuttVertxClient(vertx, keyPair, networkKeyBytes32);

    AsyncResult<RPCHandler> onConnect = secureScuttlebuttVertxClient
        .connectTo(
            port,
            host,
            keyPair.publicKey(),
            null,
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
