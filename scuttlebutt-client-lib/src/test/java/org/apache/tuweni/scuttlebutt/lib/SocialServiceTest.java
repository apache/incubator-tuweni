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

import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.crypto.sodium.Signature;
import org.apache.tuweni.scuttlebutt.lib.model.Profile;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class SocialServiceTest {

  @Test
  @Disabled("Requires an ssb instance")
  public void testViewFollowingWithPatchwork() throws Exception {

    Signature.KeyPair localKeys = KeyFileLoader.getLocalKeys();

    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult =
        ScuttlebuttClientFactory.fromNet(new ObjectMapper(), "localhost", 8008, localKeys);

    ScuttlebuttClient scuttlebuttClient = scuttlebuttClientLibAsyncResult.get();

    AsyncResult<List<Profile>> following = scuttlebuttClient.getSocialService().getFollowing();

    List<Profile> profiles = following.get(10, TimeUnit.SECONDS);

    assertTrue(!profiles.isEmpty());

    profiles.stream().forEach(profile -> System.out.println(profile.getDisplayName()));

    System.out.println("Following: " + profiles.size());
  }

  @Test
  @Disabled("Requires an ssb instance")
  public void testViewFollowedByPatchwork() throws Exception {
    Signature.KeyPair localKeys = KeyFileLoader.getLocalKeys();

    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult =
        ScuttlebuttClientFactory.fromNet(new ObjectMapper(), "localhost", 8008, localKeys);

    ScuttlebuttClient scuttlebuttClient = scuttlebuttClientLibAsyncResult.get();

    AsyncResult<List<Profile>> followedBy = scuttlebuttClient.getSocialService().getFollowedBy();

    List<Profile> profiles = followedBy.get(30, TimeUnit.SECONDS);

    assertTrue(!profiles.isEmpty());

    profiles.stream().forEach(profile -> System.out.println(profile.getDisplayName()));

    System.out.println("Followed by: " + profiles.size());
  }

  @Test
  @Disabled("Requires an ssb instance")
  public void testFriendsWithPatchwork() throws Exception {
    Signature.KeyPair localKeys = KeyFileLoader.getLocalKeys();

    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult =
        ScuttlebuttClientFactory.fromNet(new ObjectMapper(), "localhost", 8008, localKeys);

    ScuttlebuttClient scuttlebuttClient = scuttlebuttClientLibAsyncResult.get();

    AsyncResult<List<Profile>> friends = scuttlebuttClient.getSocialService().getFriends();

    List<Profile> profiles = friends.get(30, TimeUnit.SECONDS);

    assertTrue(!profiles.isEmpty());

    profiles.stream().forEach(profile -> System.out.println(profile.getDisplayName()));

    System.out.println("Friends: " + profiles.size());
  }

  @Test
  @Disabled("Requires an ssb instance")
  public void testSetDisplayName() throws Exception {

    TestConfig testConfig = TestConfig.fromEnvironment();

    AsyncResult<ScuttlebuttClient> scuttlebuttClientLibAsyncResult = ScuttlebuttClientFactory
        .fromNet(new ObjectMapper(), testConfig.getHost(), testConfig.getPort(), testConfig.getKeyPair());

    ScuttlebuttClient scuttlebuttClient = scuttlebuttClientLibAsyncResult.get();

    SocialService socialService = scuttlebuttClient.getSocialService();

    String newDisplayName = "Test display name";

    Profile profile = socialService.setDisplayName(newDisplayName).get();

    assertEquals(profile.getDisplayName(), newDisplayName);

    Profile ownProfile = socialService.getOwnProfile().get();

    assertEquals(ownProfile.getDisplayName(), newDisplayName);
  }


}
