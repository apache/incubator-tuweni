// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.scuttlebutt.lib

import org.apache.tuweni.scuttlebutt.lib.model.ScuttlebuttMessageContent

internal class TestScuttlebuttSerializationModel : ScuttlebuttMessageContent {
  var value: String? = null
    private set
  override val type = "serialization-test"

  constructor() {}
  constructor(value: String?) {
    this.value = value
  }
}
