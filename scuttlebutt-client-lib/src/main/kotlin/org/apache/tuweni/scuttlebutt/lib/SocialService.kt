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

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.scuttlebutt.lib.model.Profile
import org.apache.tuweni.scuttlebutt.lib.model.UpdateNameMessage
import org.apache.tuweni.scuttlebutt.lib.model.query.AboutQuery
import org.apache.tuweni.scuttlebutt.lib.model.query.AboutQueryResponse
import org.apache.tuweni.scuttlebutt.lib.model.query.IsFollowingQuery
import org.apache.tuweni.scuttlebutt.lib.model.query.IsFollowingResponse
import org.apache.tuweni.scuttlebutt.lib.model.query.WhoAmIResponse
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction
import org.apache.tuweni.scuttlebutt.rpc.mux.Multiplexer
import java.util.stream.Collectors

/**
 * Operations for querying the follow graph, and fetching the profiles of users.
 *
 * Assumes that the standard 'ssb-about' and 'ssb-friends' plugins are installed on the target instance (or that RPC
 * functions meeting their manifests' contracts are available.)
 *
 * Should not be instantiated directly - an instance should be acquired via the ScuttlebuttClient instance
 *
 */
class SocialService(private val multiplexer: Multiplexer, private val feedService: FeedService) {
  companion object {
    private val mapper: ObjectMapper = ObjectMapper()
  }

  /**
   * Get the instance's public key (the key used for its identity.)
   *
   * @return the instance's public key
   */
  suspend fun getOwnID(): String {
    val function = RPCFunction("whoami")
    val request = RPCAsyncRequest(function, emptyList())
    val response = multiplexer.makeAsyncRequest(request)
    return response.asJSON(
      mapper,
      WhoAmIResponse::class.java
    ).id
  }

  /**
   * Get the instance's current profile
   *
   * @return the instance current profile.
   */
  suspend fun getOwnProfile(): Profile {
    val id = getOwnID()
    return getProfile(id)
  }

  /**
   * A map of all the instance IDs to how many hops away they are in the social graph.
   *
   * @return map of all the instance IDs to how many hops away they are in the social graph
   */
  private suspend fun getHops(): Map<String, Int> {
    val rpcFunction = RPCFunction(listOf("friends"), "hops")
    val rpcAsyncRequest = RPCAsyncRequest(rpcFunction, listOf())
    val rpcResponse = multiplexer.makeAsyncRequest(rpcAsyncRequest)
    val followHops: Map<String, Int> = rpcResponse.asJSON(
      mapper,
      object : TypeReference<Map<String, Int>>() {}
    )
    return followHops
  }

  /**
   * Queries whether a given user (source) is following another given user (destination)
   *
   * @param source the node we want to check is following the destination
   * @param destination the destination node
   * @return whether source follows destination
   */
  private suspend fun isFollowing(source: String, destination: String): IsFollowingResponse {
    val function = RPCFunction(listOf("friends"), "isFollowing")
    val rpcAsyncRequest = RPCAsyncRequest(function, listOf<Any>(IsFollowingQuery(source, destination)))
    val rpcResponse = multiplexer.makeAsyncRequest(rpcAsyncRequest)
    val answer = rpcResponse.asJSON(mapper, Boolean::class.java)
    return IsFollowingResponse(
      source,
      destination,
      answer
    )
  }

  /**
   * Get the profiles of all the users that the instance is following.
   *
   * @return the profiles of all the users that the instance is following
   */
  suspend fun getFollowing(): List<Profile> {
    val followHops = getHops()
    val following: List<String> = followHops.keys.stream().filter { key: String -> followHops[key] == 1 }
      .collect(Collectors.toList())
    return getProfiles(following)
  }

  /**
   * Get the profiles of all the instances that are following the instance.
   *
   * @return the profiles of all the instances that are following the instance
   */
  suspend fun getFollowedBy(): List<Profile> {
    val id = getOwnID()
    val hops = getHops()
    val identities: Set<String> = hops.keys
    val results = mutableListOf<String>()
    for (ident in identities) {
      val f = isFollowing(
        ident,
        id
      )
      if (f.following) {
        results.add(
          f.source
        )
      }
    }
    return getProfiles(results)
  }

  /**
   * Get the profiles of all the users that the instance is following that also follow the instance.
   *
   * @return the profiles of all the users that the instance is following that also follow the instance
   */
  suspend fun getFriends(): List<Profile> {
    val id = getOwnID()
    val results = mutableListOf<Profile>()
    for (profile in getFollowing()) {
      val f = isFollowing(
        profile.key,
        id
      )
      if (f.following) {
        results.add(
          profile
        )
      }
    }
    return results
  }

  /**
   * Gets the profile of a given user
   *
   * @param publicKey the public key of the user to get the profile of
   * @return the profile of a given user
   */
  private suspend fun getProfile(publicKey: String): Profile {
    val function = RPCFunction(listOf("about"), "latestValues")
    val query = AboutQuery(publicKey, listOf("name"))
    val rpcAsyncRequest = RPCAsyncRequest(function, listOf<Any>(query))
    val rpcResponse = multiplexer.makeAsyncRequest(rpcAsyncRequest)
    val aboutQueryResponse: AboutQueryResponse = rpcResponse.asJSON(
      mapper,
      AboutQueryResponse::class.java
    )
    return Profile(publicKey, aboutQueryResponse.name)
  }

  /**
   * Set the display name of the instance by posting an 'about' message to the feed.
   *
   * @param displayName the instance's new display name
   * @return the new profile after setting the display name
   */
  suspend fun setDisplayName(displayName: String): Profile {
    val ownId = getOwnID()
    feedService.publish(UpdateNameMessage(displayName, ownId))
    return getProfile(ownId)
  }

  /**
   * Fetches the profiles of the given list of users.
   *
   * @param keys the users to get the profiles of
   * @return the user profiles
   */
  private suspend fun getProfiles(keys: List<String>): List<Profile> {
    val profiles = mutableListOf<Profile>()
    for (key in keys) {
      val p = getProfile(key)
      profiles.add(p)
    }
    return profiles
  }
}
