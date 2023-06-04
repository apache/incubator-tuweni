// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.eth

import org.apache.tuweni.rlpx.WireConnectionRepository
import org.apache.tuweni.rlpx.wire.WireConnection

interface ConnectionSelectionStrategy {

  fun selectConnection(): WireConnection
}

class RandomConnectionSelectionStrategy(val repository: WireConnectionRepository) : ConnectionSelectionStrategy {
  override fun selectConnection(): WireConnection = repository.asIterable().first()
}
