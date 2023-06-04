// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.stratum.server

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.units.bigints.UInt256

data class PoWInput(val target: UInt256, val prePowHash: Bytes, val blockNumber: Long)

data class PoWSolution(val nonce: Long, val mixHash: Bytes32, val solution: Bytes?, val powHash: Bytes)
