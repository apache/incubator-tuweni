// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib.model.query

/**
 * A query body to check whether the 'source' is following the 'destination' user.
 *
 * @param source the source to check
 * @param dest the target node
 */
data class IsFollowingQuery(val source: String, val dest: String)
