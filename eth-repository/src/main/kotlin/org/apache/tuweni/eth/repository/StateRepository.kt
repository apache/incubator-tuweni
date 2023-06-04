// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.eth.repository

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.eth.AccountState
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.trie.MerkleTrie
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Wei

/**
 * Repository to manage state.
 */
interface StateRepository {

  companion object {
    val EMPTY_STORAGE_HASH = Hash.fromBytes(MerkleTrie.EMPTY_TRIE_ROOT_HASH)
    val EMPTY_CODE_HASH = Hash.hash(Bytes.EMPTY)
  }

  /**
   * Retrieves an account state for a given account.
   *
   * @param address the address of the account
   * @return the account's state, or null if not found
   */
  suspend fun getAccount(address: Address): AccountState?

  /**
   * Checks if a given account is stored in the repository.
   *
   * @param address the address of the account
   * @return true if the accounts exists
   */
  suspend fun accountsExists(address: Address): Boolean

  /**
   * Gets a value stored in an account store, or null if the account doesn't exist.
   *
   * @param address the address of the account
   * @param key the key of the value to retrieve in the account storage.
   */
  suspend fun getAccountStoreValue(address: Address, key: Bytes32): Bytes?

  /**
   * Stores a value in an account store.
   *
   * @param address the address of the account
   * @param key the key of the value to retrieve in the account storage.
   * @param value the value to store
   */
  suspend fun storeAccountValue(address: Address, key: Bytes32, value: Bytes)

  /**
   * Stores a value in an account store.
   *
   * @param address the address of the account
   * @param key the key of the value to retrieve in the account storage.
   */
  suspend fun deleteAccountStore(address: Address, key: Bytes32)

  /**
   * Gets the code of an account
   *
   * @param address the address of the account
   * @return the code or null if the address doesn't exist or the code is not present.
   */
  suspend fun getAccountCode(address: Address): Bytes?

  /**
   * Provides a new account state
   *
   * @return a new blank account state
   */
  fun newAccountState(): AccountState {
    return AccountState(UInt256.ZERO, Wei.valueOf(0), EMPTY_STORAGE_HASH, EMPTY_CODE_HASH)
  }

  /**
   * Destroys an account.
   *
   * @param address the address of the account
   * @param account the account's state
   */
  suspend fun destroyAccount(address: Address)

  /**
   * Stores an account state for a given account.
   *
   * @param address the address of the account
   * @param account the account's state
   */
  suspend fun storeAccount(address: Address, account: AccountState)

  /**
   * Stores account code in world state
   *
   * @param code the code to store
   */
  suspend fun storeCode(code: Bytes)

  /**
   * Computes the root hash of the state
   *
   * @return the root hash of the state
   */
  fun stateRootHash(): Bytes32
}
