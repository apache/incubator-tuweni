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

import io.vertx.core.Vertx
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.crypto.sodium.Sodium
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.junit.VertxInstance
import org.apache.tuweni.scuttlebutt.lib.model.Profile
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Disabled("Requires a Patchwork instance")
@ExtendWith(VertxExtension::class)
class SocialServiceTest {
  companion object {
    @JvmStatic
    @BeforeAll
    fun checkSodium() {
      Assumptions.assumeTrue(Sodium.isAvailable(), "Sodium native library is not available")
    }
  }

  @Test
  @Throws(Exception::class)
  fun testViewFollowingWithPatchwork(@VertxInstance vertx: Vertx) = runBlocking {
    val scuttlebuttClient = Utils.getMasterClient(vertx)
    val profiles = scuttlebuttClient.socialService.getFollowing()
    Assertions.assertTrue(!profiles.isEmpty())
    profiles.stream().forEach { (_, displayName): Profile ->
      println(
        displayName
      )
    }
    println("Following: " + profiles.size)
  }

  @Test
  @Throws(Exception::class)
  fun testViewFollowedByPatchwork(@VertxInstance vertx: Vertx) = runBlocking {
    val scuttlebuttClient = Utils.getMasterClient(vertx)
    val profiles = scuttlebuttClient.socialService.getFollowedBy()
    Assertions.assertTrue(!profiles.isEmpty())
    profiles.stream().forEach { (_, displayName): Profile ->
      println(
        displayName
      )
    }
    println("Followed by: " + profiles.size)
  }

  @Test
  @Throws(Exception::class)
  fun testFriendsWithPatchwork(@VertxInstance vertx: Vertx) = runBlocking {
    val scuttlebuttClient = Utils.getMasterClient(vertx)
    val profiles = scuttlebuttClient.socialService.getFriends()
    Assertions.assertTrue(!profiles.isEmpty())
    profiles.stream().forEach { (_, displayName): Profile ->
      println(
        displayName
      )
    }
    println("Friends: " + profiles.size)
  }

  @Test
  @Throws(Exception::class)
  fun testSetDisplayName(@VertxInstance vertx: Vertx) = runBlocking {
    val scuttlebuttClient = Utils.getMasterClient(vertx)
    val socialService = scuttlebuttClient.socialService
    val newDisplayName = "Test display name"
    val (_, displayName) = socialService.setDisplayName(newDisplayName)
    Assertions.assertEquals(displayName, newDisplayName)
    val (_, displayName1) = socialService.getOwnProfile()
    Assertions.assertEquals(displayName1, newDisplayName)
  }
}
