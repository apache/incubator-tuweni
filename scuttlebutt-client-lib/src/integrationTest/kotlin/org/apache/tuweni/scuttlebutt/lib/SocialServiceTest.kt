// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
