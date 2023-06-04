// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib.model

/**
 * Classes that are to be posted to the scuttlebutt feed should implement this interface
 */
interface ScuttlebuttMessageContent {
  /**
   * To be serializable to scuttlebutt, its message content must have a 'type' field.
   *
   * @return the type of the message
   */
  val type: String
}
