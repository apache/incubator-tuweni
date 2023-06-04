// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.v5.topic

import org.apache.tuweni.bytes.Bytes

internal data class Topic(
  val content: String,
) {

  fun toBytes(): Bytes = Bytes.fromHexString(content)
}
