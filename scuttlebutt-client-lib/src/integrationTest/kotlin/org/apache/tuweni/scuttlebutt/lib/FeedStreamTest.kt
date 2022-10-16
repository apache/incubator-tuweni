/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.lib

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.crypto.sodium.Sodium
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.apache.tuweni.scuttlebutt.lib.model.FeedMessage
import org.apache.tuweni.scuttlebutt.lib.model.StreamHandler
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.Optional

@ExtendWith(VertxExtension::class, BouncyCastleExtension::class)
internal class FeedStreamTest {

  companion object {
    @JvmStatic
    @BeforeAll
    fun checkSodium() {
      Assumptions.assumeTrue(Sodium.isAvailable(), "Sodium native library is not available")
    }
  }

  /**
   * Tests it is possible to make posts and retrieve them again using the FeedService class
   */
  @Test
  @Throws(Exception::class)
  fun testCreateFeedStream(@VertxInstance vertx: Vertx) = runBlocking {
    val scuttlebuttClient = Utils.getMasterClient(vertx)
    val feedService = scuttlebuttClient.feedService
    val feedMessages = publishTestMessages(feedService)

    Assertions.assertEquals(feedMessages.size, 10)
    val lastPosted =
      feedMessages.stream().max(Comparator.comparingLong { (_, _, value): FeedMessage -> value.sequence })
    Assertions.assertTrue(lastPosted.isPresent)
    val lastMessage = AsyncResult.incomplete<Optional<FeedMessage>>()
    feedService.createFeedStream {
      object : StreamHandler<FeedMessage> {
        var currentMessage = Optional.empty<FeedMessage>()
        override fun onMessage(item: FeedMessage) {
          currentMessage = Optional.of(item)
        }

        override fun onStreamEnd() {
          lastMessage.complete(currentMessage)
        }

        override fun onStreamError(ex: Exception?) {
          Assertions.fail<Any>(ex!!.message)
        }
      }
    }
    val lastStreamedMessage = lastMessage.get()!!
    Assertions.assertTrue(lastStreamedMessage.isPresent)
    Assertions.assertEquals(lastStreamedMessage.get().value.sequence, lastPosted.get().value.sequence)
    val content = lastStreamedMessage.get().value.getContentAs(
      ObjectMapper(),
      TestScuttlebuttSerializationModel::class.java
    )
    Assertions.assertEquals("serialization-test", content.type)
    val result = scuttlebuttClient
      .rawRequestService
      .makeAsyncRequest(RPCAsyncRequest(RPCFunction("whoami"), emptyList()))
    assertNotNull(result)
    val map: Map<*, *> = result.asJSON(ObjectMapper(), Map::class.java)
    Assertions.assertTrue(map.containsKey("id"))
  }

  @Throws(JsonProcessingException::class)
  private suspend fun publishTestMessages(feedService: FeedService): List<FeedMessage> {
    val results: MutableList<FeedMessage> = ArrayList()
    for (i in 0..9) {
      val result: FeedMessage = feedService.publish(
        TestScuttlebuttSerializationModel(
          "test: $i"
        )
      )
      results.add(result)
    }
    return results
  }
}
