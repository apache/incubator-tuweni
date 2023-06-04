// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib.model

/**
 * A message that when persisted to the feed updates the name of the given user
 */
data class UpdateNameMessage(val about: String, val name: String) : ScuttlebuttMessageContent {
  override var type = "about"
}
