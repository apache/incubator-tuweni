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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.await

/**
 * Storage for use in a [StoredMerklePatriciaTrie].
 */
interface MerkleStorage {

  /**
   * Get the stored content under the given hash.
   *
   * @param hash The hash for the content.
   * @return The stored content, or {@code null} if not found.
   */
  suspend fun get(hash: Bytes32): Bytes?

  /**
   * Store content with a given hash.
   *
   * Note: if the storage implementation already contains content for the given hash, it does not need to replace the
   * existing content.
   *
   * @param hash The hash for the content.
   * @param content The content to store.
   */
  suspend fun put(hash: Bytes32, content: Bytes)
}

/**
 * A [MerkleStorage] implementation using [AsyncResult]'s.
 */
abstract class AsyncMerkleStorage : MerkleStorage {
  override suspend fun get(hash: Bytes32): Bytes? = getAsync(hash).await()

  /**
   * Get the stored content under the given hash.
   *
   * @param hash The hash for the content.
   * @return An [AsyncResult] that will complete with the stored content or {@code null} if not found.
   */
  abstract fun getAsync(hash: Bytes32): AsyncResult<Bytes?>

  override suspend fun put(hash: Bytes32, content: Bytes) = putAsync(hash, content).await()

  /**
   * Store content with a given hash.
   *
   * Note: if the storage implementation already contains content for the given hash, it does not need to replace the
   * existing content.
   *
   * @param hash The hash for the content.
   * @param content The content to store.
   * @return An [AsyncCompletion] that will complete when the content is stored.
   */
  abstract fun putAsync(hash: Bytes32, content: Bytes): AsyncCompletion
}
