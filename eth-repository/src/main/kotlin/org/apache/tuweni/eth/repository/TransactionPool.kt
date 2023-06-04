// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.repository

import org.apache.tuweni.concurrent.ExpiringMap
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Transaction

interface TransactionPool {

  suspend fun contains(hash: Hash): Boolean
  suspend fun get(hash: Hash): Transaction?
  suspend fun add(transaction: Transaction)
}

class MemoryTransactionPool : TransactionPool {

  val storage = ExpiringMap<Hash, Transaction>()

  override suspend fun contains(hash: Hash): Boolean = storage.containsKey(hash)

  override suspend fun get(hash: Hash) = storage.get(hash)

  override suspend fun add(transaction: Transaction) {
    storage.put(transaction.getHash(), transaction)
  }
}
