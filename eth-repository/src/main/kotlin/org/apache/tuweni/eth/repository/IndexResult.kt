// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.repository

/**
 * Result of the indexing operation for one block header.
 *
 * @param canonical true if the header has a parent hash that goes all the way back to the start of the chain.
 */
data class IndexResult(val canonical: Boolean = false)
