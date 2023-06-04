// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib

import org.apache.tuweni.scuttlebutt.rpc.mux.Multiplexer

/**
 * A client for making requests to a scuttlebutt instance with. This is the entry point for accessing service classes
 * which perform operations related to different logical areas.
 *
 * Should be constructed using the ScuttlebuttClientFactory factory class.
 *
 * @param multiplexer the multiplexer to make RPC requests with.
 */
class ScuttlebuttClient(multiplexer: Multiplexer) {
  /**
   * Provides a service for operations that concern scuttlebutt feeds.
   *
   * @return a service for operations that concern scuttlebutt feeds
   */
  val feedService = FeedService(multiplexer)

  /**
   * Provides a service for operations that connect nodes together.
   *
   * @return a service for operations that connect nodes together
   */
  val networkService = NetworkService(multiplexer)

  /**
   * Provides a service for operations concerning social connections and updating the instance's profile
   *
   * @return a service for operations concerning social connections and updating the instance's profile
   */
  val socialService = SocialService(multiplexer, feedService)

  /**
   * Provides a service for making lower level requests that are not supported by higher level services.
   *
   * @return a service for making lower level requests that are not supported by higher level services
   */
  val rawRequestService = RawRequestService(multiplexer)
}
