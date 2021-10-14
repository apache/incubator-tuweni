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
package org.apache.tuweni.trie

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.concurrent.coroutines.asyncResult
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.rlp.RLP

/**
 * A Merkle Trie.
 */
interface MerkleTrie<in K, V> : CoroutineScope {
  companion object {
    /**
     * The root hash of an empty tree.
     */
    val EMPTY_TRIE_ROOT_HASH: Bytes32 = Hash.keccak256(RLP.encodeValue(Bytes.EMPTY))
  }

  /**
   * Returns the value that corresponds to the specified key, or an empty byte array if no such value exists.
   *
   * @param key The key of the value to be returned.
   * @return The value that corresponds to the specified key, or {@code null} if no such value exists.
   * @throws MerkleStorageException If there is an error while accessing or decoding data from storage.
   */
  suspend fun get(key: K): V?

  /**
   * Returns the value that corresponds to the specified key, or an empty byte array if no such value exists.
   *
   * @param key The key of the value to be returned.
   * @return A value that corresponds to the specified key, or {@code null} if no such value exists.
   */
  fun getAsync(key: K): AsyncResult<V?> = getAsync(Dispatchers.Default, key)

  /**
   * Returns the value that corresponds to the specified key, or an empty byte array if no such value exists.
   *
   * @param key The key of the value to be returned.
   * @param dispatcher The co-routine dispatcher for asynchronous tasks.
   * @return A value that corresponds to the specified key, or {@code null} if no such value exists.
   */
  fun getAsync(dispatcher: CoroutineDispatcher, key: K): AsyncResult<V?> = asyncResult { get(key) }

  /**
   * Updates the value that corresponds to the specified key, creating the value if one does not already exist.
   *
   * If the value is null, deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key that corresponds to the value to be updated.
   * @param value The value to associate the key with.
   * @throws MerkleStorageException If there is an error while writing to storage.
   */
  suspend fun put(key: K, value: V?)

  /**
   * Updates the value that corresponds to the specified key, creating the value if one does not already exist.
   *
   * If the value is null, deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key that corresponds to the value to be updated.
   * @param value The value to associate the key with.
   * @return A completion that will complete when the value has been put into the trie.
   */
  fun putAsync(key: K, value: V?): AsyncCompletion = putAsync(Dispatchers.Default, key, value)

  /**
   * Updates the value that corresponds to the specified key, creating the value if one does not already exist.
   *
   * If the value is null, deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key that corresponds to the value to be updated.
   * @param value The value to associate the key with.
   * @param dispatcher The co-routine dispatcher for asynchronous tasks.
   * @return A completion that will complete when the value has been put into the trie.
   */
  fun putAsync(dispatcher: CoroutineDispatcher, key: K, value: V?): AsyncCompletion =
    asyncCompletion { put(key, value) }

  /**
   * Deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key of the value to be deleted.
   * @throws MerkleStorageException If there is an error while writing to storage.
   */
  suspend fun remove(key: K)

  /**
   * Deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key of the value to be deleted.
   * @return A completion that will complete when the value has been removed.
   */
  fun removeAsync(key: K): AsyncCompletion = removeAsync(Dispatchers.Default, key)

  /**
   * Deletes the value that corresponds to the specified key, if such a value exists.
   *
   * @param key The key of the value to be deleted.
   * @param dispatcher The co-routine dispatcher for asynchronous tasks.
   * @return A completion that will complete when the value has been removed.
   */
  fun removeAsync(dispatcher: CoroutineDispatcher, key: K): AsyncCompletion = asyncCompletion { remove(key) }

  /**
   * Returns the KECCAK256 hash of the root node of the trie.
   *
   * @return The KECCAK256 hash of the root node of the trie.
   */
  fun rootHash(): Bytes32
}
