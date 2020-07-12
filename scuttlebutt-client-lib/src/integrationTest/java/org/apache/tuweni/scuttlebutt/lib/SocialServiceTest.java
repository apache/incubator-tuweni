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

import static org.apache.tuweni.scuttlebutt.lib.Utils.getMasterClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.sodium.Sodium;
import org.apache.tuweni.junit.VertxExtension;
import org.apache.tuweni.junit.VertxInstance;
import org.apache.tuweni.scuttlebutt.lib.model.Profile;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Disabled("Requires a Patchwork instance")
@ExtendWith(VertxExtension.class)
public class SocialServiceTest {

  @BeforeAll
  static void checkSodium() {
    assumeTrue(Sodium.isAvailable(), "Sodium native library is not available");
  }

  @Test
  void testViewFollowingWithPatchwork(@VertxInstance Vertx vertx) throws Exception {
    ScuttlebuttClient scuttlebuttClient = getMasterClient(vertx);

    AsyncResult<List<Profile>> following = scuttlebuttClient.getSocialService().getFollowing();

    List<Profile> profiles = following.get(10, TimeUnit.SECONDS);

    assertTrue(!profiles.isEmpty());

    profiles.stream().forEach(profile -> System.out.println(profile.getDisplayName()));

    System.out.println("Following: " + profiles.size());
  }

  @Test
  void testViewFollowedByPatchwork(@VertxInstance Vertx vertx) throws Exception {
    ScuttlebuttClient scuttlebuttClient = getMasterClient(vertx);

    AsyncResult<List<Profile>> followedBy = scuttlebuttClient.getSocialService().getFollowedBy();

    List<Profile> profiles = followedBy.get(30, TimeUnit.SECONDS);

    assertTrue(!profiles.isEmpty());

    profiles.stream().forEach(profile -> System.out.println(profile.getDisplayName()));

    System.out.println("Followed by: " + profiles.size());
  }

  @Test
  void testFriendsWithPatchwork(@VertxInstance Vertx vertx) throws Exception {

    ScuttlebuttClient scuttlebuttClient = getMasterClient(vertx);

    AsyncResult<List<Profile>> friends = scuttlebuttClient.getSocialService().getFriends();

    List<Profile> profiles = friends.get(30, TimeUnit.SECONDS);

    assertTrue(!profiles.isEmpty());

    profiles.stream().forEach(profile -> System.out.println(profile.getDisplayName()));

    System.out.println("Friends: " + profiles.size());
  }

  @Test
  void testSetDisplayName(@VertxInstance Vertx vertx) throws Exception {

    ScuttlebuttClient scuttlebuttClient = getMasterClient(vertx);

    SocialService socialService = scuttlebuttClient.getSocialService();

    String newDisplayName = "Test display name";

    Profile profile = socialService.setDisplayName(newDisplayName).get();

    assertEquals(profile.getDisplayName(), newDisplayName);

    Profile ownProfile = socialService.getOwnProfile().get();

    assertEquals(ownProfile.getDisplayName(), newDisplayName);
  }


}
