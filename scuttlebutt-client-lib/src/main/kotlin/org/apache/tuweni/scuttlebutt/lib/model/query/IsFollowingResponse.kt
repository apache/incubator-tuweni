// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib.model.query

/**
 * A response to a query on whether 'source' is following 'destination'
 *
 * @param source the source node
 * @param destination the destination node
 * @param following true if source is following destination, false otherwise
 */
data class IsFollowingResponse(val source: String, val destination: String, val following: Boolean)
