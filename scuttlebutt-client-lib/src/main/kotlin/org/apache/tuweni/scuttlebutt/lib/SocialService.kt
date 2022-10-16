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
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.scuttlebutt.lib.model.Profile
import org.apache.tuweni.scuttlebutt.lib.model.UpdateNameMessage
import org.apache.tuweni.scuttlebutt.lib.model.query.AboutQuery
import org.apache.tuweni.scuttlebutt.lib.model.query.AboutQueryResponse
import org.apache.tuweni.scuttlebutt.lib.model.query.IsFollowingQuery
import org.apache.tuweni.scuttlebutt.lib.model.query.IsFollowingResponse
import org.apache.tuweni.scuttlebutt.lib.model.query.WhoAmIResponse
import org.apache.tuweni.scuttlebutt.rpc.RPCAsyncRequest
import org.apache.tuweni.scuttlebutt.rpc.RPCFunction
import org.apache.tuweni.scuttlebutt.rpc.RPCResponse
import org.apache.tuweni.scuttlebutt.rpc.mux.Multiplexer
import java.io.IOException
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
  private val mapper: ObjectMapper = ObjectMapper()

  /**
   * Get the instance's public key (the key used for its identity.)
   *
   * @return the instance's public key
   */
  val ownIdentity: AsyncResult<String>
    get() {
      val function = RPCFunction("whoami")
      val request = RPCAsyncRequest(function, emptyList())
      try {
        return multiplexer.makeAsyncRequest(request).then { response: RPCResponse ->
          try {
            return@then AsyncResult.completed(
              response.asJSON(
                mapper,
                WhoAmIResponse::class.java
              ).id
            )
          } catch (e: IOException) {
            return@then AsyncResult.exceptional<String>(e)
          }
        }
      } catch (e: JsonProcessingException) {
        return AsyncResult.exceptional(e)
      }
    }

  /**
   * Get the instance's current profile
   *
   * @return the instance current profile.
   */
  val ownProfile: AsyncResult<Profile>
    get() = ownIdentity.then { publicKey: String ->
      getProfile(
        publicKey
      )
    }

  /**
   * Get the profiles of all the users that the instance is following.
   *
   * @return the profiles of all the users that the instance is following
   */
  val following: AsyncResult<List<Profile>>
    get() {
      return hops.then { followHops: Map<String, Int> ->
        val following: List<String> = followHops.keys.stream().filter { key: String -> followHops[key] == 1 }
          .collect(Collectors.toList())
        getProfiles(following)
      }
    }

  /**
   * Get the profiles of all the instances that are following the instance.
   *
   * @return the profiles of all the instances that are following the instance
   */
  val followedBy: AsyncResult<List<Profile>>
    get() {
      return ownIdentity.then { ownIdentity: String ->
        hops.then { hops: Map<String, Int> ->
          val identities: Set<String> = hops.keys
          val results: List<AsyncResult<IsFollowingResponse>> =
            identities.stream().map { ident: String ->
              isFollowing(
                ident,
                ownIdentity
              )
            }
              .collect(Collectors.toList())
          val allResults: AsyncResult<List<IsFollowingResponse>> =
            AsyncResult.combine(results)
          val ids: AsyncResult<List<String>> = allResults
            .thenApply { queryResults: List<IsFollowingResponse> ->
              queryResults
                .stream()
                .filter { obj: IsFollowingResponse -> obj.following }
                .map { obj: IsFollowingResponse -> obj.source }
                .collect(Collectors.toList())
            }
          ids.then { keys: List<String> ->
            getProfiles(
              keys
            )
          }
        }
      }
    }

  /**
   * Get the profiles of all the users that the instance is following that also follow the instance.
   *
   * @return the profiles of all the users that the instance is following that also follow the instance
   */
  val friends: AsyncResult<List<Profile>>
    get() {
      return ownIdentity.then { ident: String ->
        following.then { following: List<Profile> ->
          val responses: List<AsyncResult<IsFollowingResponse>> =
            following.stream().map { follow: Profile ->
              isFollowing(
                follow.key,
                ident
              )
            }
              .collect(Collectors.toList())
          AsyncResult.combine(responses)
            .then { response: List<IsFollowingResponse> ->
              val profiles: List<AsyncResult<Profile>> =
                response
                  .stream()
                  .filter { obj: IsFollowingResponse -> obj.following }
                  .map { item: IsFollowingResponse ->
                    getProfile(
                      item.source
                    )
                  }
                  .collect(
                    Collectors.toList()
                  )
              AsyncResult.combine(
                profiles
              )
            }
        }
      }
    }

  /**
   * Gets the profile of a given user
   *
   * @param publicKey the public key of the user to get the profile of
   * @return the profile of a given user
   */
  private fun getProfile(publicKey: String): AsyncResult<Profile> {
    val function = RPCFunction(listOf("about"), "latestValues")
    val query = AboutQuery(publicKey, listOf("name"))
    val rpcAsyncRequest = RPCAsyncRequest(function, listOf<Any>(query))
    try {
      val rpcResponseAsyncResult: AsyncResult<RPCResponse> = multiplexer.makeAsyncRequest(rpcAsyncRequest)
      return rpcResponseAsyncResult.then { rpcResponse: RPCResponse ->
        try {
          val aboutQueryResponse: AboutQueryResponse = rpcResponse.asJSON(
            mapper,
            AboutQueryResponse::class.java
          )
          return@then AsyncResult.completed(
            Profile(publicKey, aboutQueryResponse.name)
          )
        } catch (e: IOException) {
          return@then AsyncResult.exceptional<Profile>(
            e
          )
        }
      }
    } catch (e: JsonProcessingException) {
      return AsyncResult.exceptional(e)
    }
  }

  /**
   * Set the display name of the instance by posting an 'about' message to the feed.
   *
   * @param displayName the instance's new display name
   * @return the new profile after setting the display name
   */
  fun setDisplayName(displayName: String): AsyncResult<Profile> {
    return ownIdentity.then { ownId: String ->
      try {
        return@then feedService.publish(UpdateNameMessage(displayName, ownId))
          .then {
            getProfile(
              ownId
            )
          }
      } catch (e: JsonProcessingException) {
        return@then AsyncResult.exceptional<Profile>(
          e
        )
      }
    }
  }

  /**
   * A map of all the instance IDs to how many hops away they are in the social graph.
   *
   * @return map of all the instance IDs to how many hops away they are in the social graph
   */
  private val hops: AsyncResult<Map<String, Int>>
    get() {
      val rpcFunction = RPCFunction(listOf("friends"), "hops")
      val rpcAsyncRequest = RPCAsyncRequest(rpcFunction, listOf())
      try {
        val rpcResponseAsyncResult: AsyncResult<RPCResponse> = multiplexer.makeAsyncRequest(rpcAsyncRequest)
        return rpcResponseAsyncResult.then { rpcResponse: RPCResponse ->
          try {
            val followHops: Map<String, Int> = rpcResponse.asJSON(
              mapper,
              object : TypeReference<Map<String, Int>>() {}
            )
            return@then AsyncResult.completed(
              followHops
            )
          } catch (e: IOException) {
            return@then AsyncResult.exceptional<Map<String, Int>>(e)
          }
        }
      } catch (e: JsonProcessingException) {
        return AsyncResult.exceptional(e)
      }
    }

  /**
   * Queries whether a given user (source) is following another given user (destination)
   *
   * @param source the node we want to check is following the destination
   * @param destination the destination node
   * @return whether source follows destination
   */
  private fun isFollowing(source: String, destination: String): AsyncResult<IsFollowingResponse> {
    val function = RPCFunction(listOf("friends"), "isFollowing")
    val rpcAsyncRequest = RPCAsyncRequest(function, listOf<Any>(IsFollowingQuery(source, destination)))
    try {
      return multiplexer.makeAsyncRequest(rpcAsyncRequest).then { rpcResponse: RPCResponse ->
        try {
          val answer: Boolean = rpcResponse.asJSON(mapper, Boolean::class.java)
          return@then AsyncResult.completed(
            IsFollowingResponse(
              source,
              destination,
              answer
            )
          )
        } catch (e: IOException) {
          return@then AsyncResult.exceptional<IsFollowingResponse>(e)
        }
      }
    } catch (e: JsonProcessingException) {
      return AsyncResult.exceptional(e)
    }
  }

  /**
   * Fetches the profiles of the given list of users.
   *
   * @param keys the users to get the profiles of
   * @return the user profiles
   */
  private fun getProfiles(keys: List<String>): AsyncResult<List<Profile>> {
    val asyncResultStream: List<AsyncResult<Profile>> = keys.stream().map { publicKey: String ->
      getProfile(
        publicKey
      )
    }.collect(Collectors.toList())
    return AsyncResult.combine(asyncResultStream)
  }
}
