// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.ethstats

import org.apache.tuweni.eth.Hash

/**
 * Stats reported to ethnetstats representing the hash of a transaction.
 */
data class TxStats(val hash: Hash)
