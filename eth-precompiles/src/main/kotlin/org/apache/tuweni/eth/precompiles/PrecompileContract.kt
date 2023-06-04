// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.precompiles

import org.apache.tuweni.bytes.Bytes

/**
 * Result of the execution of the precompile contract.
 */
data class Result(val gas: Long, val output: Bytes?)

/**
 * Precompile contract
 */
interface PrecompileContract {
  fun run(input: Bytes): Result
}
