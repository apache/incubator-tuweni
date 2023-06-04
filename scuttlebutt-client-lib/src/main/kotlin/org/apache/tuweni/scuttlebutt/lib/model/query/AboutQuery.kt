// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib.model.query

/**
 * Query to request details of the server
 *
 * @param dest the object that the 'about' type message refers to (e.g. user profile / message ID.)
 * @param keys The keys for the 'about' type messages that we're querying
 */
data class AboutQuery(val dest: String, val keys: List<String>)
