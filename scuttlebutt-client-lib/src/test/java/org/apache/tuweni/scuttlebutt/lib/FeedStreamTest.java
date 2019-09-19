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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.concurrent.CompletableAsyncResult;
import org.apache.tuweni.scuttlebutt.lib.model.FeedMessage;
import org.apache.tuweni.scuttlebutt.lib.model.StreamHandler;
import org.apache.tuweni.scuttlebutt.rpc.mux.exceptions.ConnectionClosedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class FeedStreamTest {

  /**
   * Tests it is possible to make posts and retrieve them again using the FeedService class
   */
  @Test
  @Disabled("Requires a running ssb server")
  public void testCreateFeedStream() throws IOException, InterruptedException, ConnectionClosedException {
    TestConfig config = TestConfig.fromEnvironment();

    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult =
        ScuttlebuttClientFactory.fromNet(new ObjectMapper(), config.getHost(), config.getPort(), config.getKeyPair());

    ScuttlebuttClient scuttlebuttClient = scuttlebuttClientLibAsyncResult.get();

    FeedService feedService = scuttlebuttClient.getFeedService();

    AsyncResult<List<FeedMessage>> published = publishTestMessages(feedService);

    // Wait for the messages to be published.
    List<FeedMessage> feedMessages = published.get();

    assertEquals(feedMessages.size(), 10);

    Optional<FeedMessage> lastPosted =
        feedMessages.stream().max(Comparator.comparingLong(msg -> msg.getValue().getSequence()));

    assertTrue(lastPosted.isPresent());

    FeedMessage lastFeedMessagePosted = lastPosted.get();

    CompletableAsyncResult<Optional<FeedMessage>> lastMessage = AsyncResult.incomplete();

    feedService.createFeedStream((closer) -> new StreamHandler<FeedMessage>() {

      Optional<FeedMessage> currentMessage = Optional.empty();

      @Override
      public void onMessage(FeedMessage item) {
        currentMessage = Optional.of(item);
      }

      @Override
      public void onStreamEnd() {
        lastMessage.complete(currentMessage);
      }

      @Override
      public void onStreamError(Exception ex) {
        System.out.println(ex.getMessage());
        fail(ex.getMessage());
      }
    });

    Optional<FeedMessage> lastStreamedMessage = lastMessage.get();

    assertTrue(lastStreamedMessage.isPresent());

    assertEquals(lastStreamedMessage.get().getValue().getSequence(), lastPosted.get().getValue().getSequence());

    TestScuttlebuttSerializationModel content =
        lastStreamedMessage.get().getValue().getContentAs(new ObjectMapper(), TestScuttlebuttSerializationModel.class);

    assertEquals("serialization-test", content.getType());
  }

  private AsyncResult<List<FeedMessage>> publishTestMessages(FeedService feedService) throws JsonProcessingException {

    List<AsyncResult<FeedMessage>> results = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      AsyncResult<FeedMessage> result = feedService.publish(new TestScuttlebuttSerializationModel("test: " + i));

      results.add(result);
    }

    return AsyncResult.combine(results);
  }

}
