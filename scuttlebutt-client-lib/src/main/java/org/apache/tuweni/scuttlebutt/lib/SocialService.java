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

import org.apache.tuweni.concurrent.AsyncResult;
import org.apache.tuweni.scuttlebutt.lib.model.Profile;
import org.apache.tuweni.scuttlebutt.lib.model.UpdateNameMessage;
import org.apache.tuweni.scuttlebutt.lib.model.query.AboutQuery;
import org.apache.tuweni.scuttlebutt.lib.model.query.AboutQueryResponse;
import org.apache.tuweni.scuttlebutt.lib.model.query.IsFollowingQuery;
import org.apache.tuweni.scuttlebutt.lib.model.query.IsFollowingResponse;
import org.apache.tuweni.scuttlebutt.lib.model.query.WhoAmIResponse;
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest;
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction;
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse;
import org.apache.tuweni.scuttlebutt.rpc.mux.Multiplexer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Operations for querying the follow graph, and fetching the profiles of users.
 *
 * Assumes that the standard 'ssb-about' and 'ssb-friends' plugins are installed on the target instance (or that RPC
 * functions meeting their manifests' contracts are available.)
 *
 * Should not be instantiated directly - an instance should be acquired via the ScuttlebuttClient instance
 *
 */
public class SocialService {

  private final Multiplexer multiplexer;
  private final FeedService feedService;

  private ObjectMapper mapper = new ObjectMapper();

  protected SocialService(Multiplexer multiplexer, FeedService feedService) {
    this.multiplexer = multiplexer;
    this.feedService = feedService;
  }

  /**
   * Get the instance's public key (the key used for its identity.)
   *
   * @return
   */
  public AsyncResult<String> getOwnIdentity() {
    RPCFunction function = new RPCFunction("whoami");

    RPCAsyncRequest request = new RPCAsyncRequest(function, Arrays.asList());

    try {
      return multiplexer.makeAsyncRequest(request).then(response -> {
        try {
          return AsyncResult.completed(response.asJSON(mapper, WhoAmIResponse.class).getId());
        } catch (IOException e) {
          return AsyncResult.exceptional(e);
        }
      });
    } catch (JsonProcessingException e) {
      return AsyncResult.exceptional(e);
    }
  }

  /**
   * Get the instance's current profile
   *
   * @return
   */
  public AsyncResult<Profile> getOwnProfile() {
    return getOwnIdentity().then(identity -> getProfile(identity));
  }

  /**
   * Get the profiles of all the users that the instance is following.
   *
   * @return
   */
  public AsyncResult<List<Profile>> getFollowing() {
    return getHops().then(followHops -> {
      List<String> following =
          followHops.keySet().stream().filter(key -> followHops.get(key) == 1).collect(Collectors.toList());

      return getProfiles(following);
    });
  }

  /**
   * Get the profiles of all the instances that are following the instance.
   *
   * @return
   */
  public AsyncResult<List<Profile>> getFollowedBy() {

    return getOwnIdentity().then(ownIdentity -> getHops().then(hops -> {
      Set<String> identities = hops.keySet();

      List<AsyncResult<IsFollowingResponse>> results =
          identities.stream().map((ident) -> isFollowing(ident, ownIdentity)).collect(Collectors.toList());

      AsyncResult<List<IsFollowingResponse>> allResults = AsyncResult.combine(results);

      AsyncResult<List<String>> ids = allResults.thenApply(
          queryResults -> queryResults
              .stream()
              .filter(result -> result.isFollowing())
              .map(IsFollowingResponse::getSource)
              .collect(Collectors.toList()));

      return ids.then(this::getProfiles);
    }));
  }

  /**
   * Get the profiles of all the users that the instance is following that also follow the instance.
   *
   * @return
   */
  public AsyncResult<List<Profile>> getFriends() {

    return getOwnIdentity().then(ident -> getFollowing().then(following -> {

      List<AsyncResult<IsFollowingResponse>> responses =
          following.stream().map((follow) -> isFollowing(follow.getKey(), ident)).collect(Collectors.toList());

      return AsyncResult.combine(responses).then(response -> {
        List<AsyncResult<Profile>> profiles =
            response.stream().filter(f -> f.isFollowing()).map(item -> getProfile(item.getSource())).collect(
                Collectors.toList());

        return AsyncResult.combine(profiles);
      });

    }));

  }

  /**
   * Gets the profile of a given user
   *
   * @param publicKey the public key of the user to get the profile of
   * @return
   */
  public AsyncResult<Profile> getProfile(String publicKey) {

    RPCFunction function = new RPCFunction(Arrays.asList("about"), "latestValues");
    AboutQuery query = new AboutQuery(publicKey, Arrays.asList("name"));
    RPCAsyncRequest rpcAsyncRequest = new RPCAsyncRequest(function, Arrays.asList(query));

    try {
      AsyncResult<RPCResponse> rpcResponseAsyncResult = multiplexer.makeAsyncRequest(rpcAsyncRequest);

      return rpcResponseAsyncResult.then(rpcResponse -> {
        try {
          AboutQueryResponse aboutQueryResponse = rpcResponse.asJSON(mapper, AboutQueryResponse.class);
          return AsyncResult.completed(new Profile(publicKey, aboutQueryResponse.getName()));
        } catch (IOException e) {
          return AsyncResult.exceptional(e);
        }
      });

    } catch (JsonProcessingException e) {
      return AsyncResult.exceptional(e);
    }
  }

  /**
   * Set the display name of the instance by posting an 'about' message to the feed.
   *
   * @param displayName the instance's new display name
   * @return the new profile after setting the display name
   */
  public AsyncResult<Profile> setDisplayName(String displayName) {

    return getOwnIdentity().then(ownId -> {
      try {
        return feedService.publish(new UpdateNameMessage(displayName, ownId)).then(feedMessage -> getProfile(ownId));
      } catch (JsonProcessingException e) {
        return AsyncResult.exceptional(e);
      }
    });
  }

  /**
   * A map of all the instance IDs to how many hops away they are in the social graph.
   *
   * @return
   */
  private AsyncResult<Map<String, Integer>> getHops() {
    RPCFunction rpcFunction = new RPCFunction(Arrays.asList("friends"), "hops");
    RPCAsyncRequest rpcAsyncRequest = new RPCAsyncRequest(rpcFunction, Arrays.asList());

    try {
      AsyncResult<RPCResponse> rpcResponseAsyncResult = multiplexer.makeAsyncRequest(rpcAsyncRequest);

      return rpcResponseAsyncResult.then(rpcResponse -> {

        try {
          Map<String, Integer> followHops = rpcResponse.asJSON(mapper, new TypeReference<Map<String, Integer>>() {});
          return AsyncResult.completed(followHops);
        } catch (IOException e) {
          return AsyncResult.exceptional(e);
        }
      });

    } catch (JsonProcessingException e) {
      return AsyncResult.exceptional(e);
    }
  }

  /**
   * Queries whether a given user (source) is following another given user (destination)
   *
   * @param source the node we want to check is following the destination
   * @param destination the destination node
   * @return
   */
  private AsyncResult<IsFollowingResponse> isFollowing(String source, String destination) {
    RPCFunction function = new RPCFunction(Arrays.asList("friends"), "isFollowing");

    RPCAsyncRequest rpcAsyncRequest =
        new RPCAsyncRequest(function, Arrays.asList(new IsFollowingQuery(source, destination)));

    try {
      return multiplexer.makeAsyncRequest(rpcAsyncRequest).then(rpcResponse -> {
        try {
          boolean answer = rpcResponse.asJSON(mapper, Boolean.class);
          return AsyncResult.completed(new IsFollowingResponse(source, destination, answer));
        } catch (IOException e) {
          return AsyncResult.exceptional(e);
        }
      });
    } catch (JsonProcessingException e) {
      return AsyncResult.exceptional(e);
    }
  }

  /**
   * Fetches the profiles of the given list of users.
   *
   * @param keys the users to get the profiles of
   * @return
   */
  private AsyncResult<List<Profile>> getProfiles(List<String> keys) {

    List<AsyncResult<Profile>> asyncResultStream = keys.stream().map(this::getProfile).collect(Collectors.toList());

    return AsyncResult.combine(asyncResultStream);
  }

}
