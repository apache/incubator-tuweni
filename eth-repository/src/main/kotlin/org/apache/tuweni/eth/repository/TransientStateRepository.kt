/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.eth.repository

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.AccountState
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.kv.CascadingKeyValueStore
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.trie.MerkleStorage
import org.apache.tuweni.trie.StoredMerklePatriciaTrie

/**
 * A state repository that keeps changes made to the underlying storage in memory.
 */
class TransientStateRepository(val repository: BlockchainRepository) : StateRepository {

  val transientWorldStateStore = MapKeyValueStore<Bytes, Bytes>()

  val transientState = CascadingKeyValueStore(transientWorldStateStore, repository.stateStore)
  val transientWorldState: StoredMerklePatriciaTrie<Bytes>

  init {
    val stateRoot = repository.worldState!!.rootHash()
    transientWorldState = StoredMerklePatriciaTrie.storingBytes(
      object : MerkleStorage {
        override suspend fun get(hash: Bytes32): Bytes? {
          return transientState.get(hash)
        }

        override suspend fun put(hash: Bytes32, content: Bytes) {
          transientState.put(hash, content)
        }
      },
      stateRoot
    )
  }

  override suspend fun getAccount(address: Address): AccountState? =
    transientWorldState.get(Hash.hash(address))?.let { AccountState.fromBytes(it) }

  override suspend fun accountsExists(address: Address): Boolean {
    return null != getAccount(address)
  }

  override suspend fun getAccountStoreValue(address: Address, key: Bytes32): Bytes? {
    val accountState = getAccount(address) ?: return null
    val tree = StoredMerklePatriciaTrie.storingBytes(
      object : MerkleStorage {
        override suspend fun get(hash: Bytes32): Bytes? {
          return transientState.get(hash)
        }

        override suspend fun put(hash: Bytes32, content: Bytes) {
          transientState.put(hash, content)
        }
      },
      accountState.storageRoot
    )
    return tree.get(key)
  }

  override suspend fun storeAccountValue(address: Address, key: Bytes32, value: Bytes) {
    val addrHash = Hash.hash(address)
    val accountState = transientWorldState.get(addrHash)?.let { AccountState.fromBytes(it) } ?: newAccountState()
    val tree = StoredMerklePatriciaTrie.storingBytes(
      object : MerkleStorage {
        override suspend fun get(hash: Bytes32): Bytes? {
          return transientState.get(hash)
        }

        override suspend fun put(hash: Bytes32, content: Bytes) {
          transientState.put(hash, content)
        }
      },
      accountState.storageRoot
    )
    tree.put(key, value)
    val newAccountState = AccountState(accountState.nonce, accountState.balance, Hash.fromBytes(tree.rootHash()), accountState.codeHash)
    transientWorldState.put(addrHash, newAccountState.toBytes())
  }

  override suspend fun deleteAccountStore(address: Address, key: Bytes32) {
    val addrHash = Hash.hash(address)
    val accountState = transientWorldState.get(addrHash)?.let { AccountState.fromBytes(it) } ?: newAccountState()
    val tree = StoredMerklePatriciaTrie.storingBytes(
      object : MerkleStorage {
        override suspend fun get(hash: Bytes32): Bytes? {
          return transientState.get(hash)
        }

        override suspend fun put(hash: Bytes32, content: Bytes) {
          transientState.put(hash, content)
        }
      },
      accountState.storageRoot
    )
    tree.remove(key)
    val newAccountState = AccountState(accountState.nonce, accountState.balance, Hash.fromBytes(tree.rootHash()), accountState.codeHash)
    transientWorldState.put(addrHash, newAccountState.toBytes())
  }

  override suspend fun getAccountCode(address: Address): Bytes? {
    val addressHash = Hash.hash(address)
    val accountStateBytes = transientWorldState.get(addressHash)
    if (accountStateBytes == null) {
      return null
    }
    val accountState = AccountState.fromBytes(accountStateBytes)
    return transientState.get(accountState.codeHash)
  }

  override suspend fun destroyAccount(address: Address) {
    transientWorldState.remove(Hash.hash(address))
  }

  override suspend fun storeAccount(address: Address, account: AccountState) {
    transientWorldState.put(Hash.hash(address), account.toBytes())
  }

  override suspend fun storeCode(code: Bytes) {
    transientState.put(Hash.hash(code), code)
  }

  override fun stateRootHash(): Bytes32 = transientWorldState.rootHash()

  /**
   * Apply changes of this repository to the blockchain repository.
   */
  suspend fun applyChanges() {
    transientState.applyChanges()
  }

  suspend fun dump(maxAccounts: Int): Set<Bytes> = transientWorldState.collect(maxAccounts) {
    try {
      AccountState.fromBytes(it)
      true
    } catch (e: Exception) {
      false
    }
  }
}
