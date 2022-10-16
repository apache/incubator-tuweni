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

import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.trie.CompactEncoding.bytesToPath
import org.apache.tuweni.trie.MerkleTrie.Companion.EMPTY_TRIE_ROOT_HASH
import java.util.function.Function
import kotlin.coroutines.CoroutineContext

/**
 * A [MerkleTrie] that persists trie nodes to a [MerkleStorage] key/value store.
 *
 * @param <V> The type of values stored by this trie.
 */
class StoredMerklePatriciaTrie<V> : MerkleTrie<Bytes, V> {

  companion object {
    /**
     * Create a trie with value of type [Bytes].
     *
     * @param storage The storage to use for persistence.
     */
    @JvmStatic
    fun storingBytes(storage: MerkleStorage): StoredMerklePatriciaTrie<Bytes> =
      StoredMerklePatriciaTrie(storage, ::bytesIdentity, ::bytesIdentity)

    /**
     * Create a trie with keys and values of type [Bytes].
     *
     * @param storage The storage to use for persistence.
     * @param rootHash The initial root has for the trie, which should be already present in `storage`.
     */
    @JvmStatic
    fun storingBytes(storage: MerkleStorage, rootHash: Bytes32): StoredMerklePatriciaTrie<Bytes> =
      StoredMerklePatriciaTrie(storage, rootHash, ::bytesIdentity, ::bytesIdentity)

    /**
     * Create a trie with keys and values of type [Bytes32].
     *
     * @param storage The storage to use for persistence.
     * @param rootHash The initial root has for the trie, which should be already present in `storage`.
     */
    @JvmStatic
    fun storingBytes32(storage: MerkleStorage, rootHash: Bytes32): StoredMerklePatriciaTrie<Bytes32> =
      StoredMerklePatriciaTrie(storage, rootHash, ::bytes32Identity) { b -> Bytes32.wrap(b) }

    /**
     * Create a trie with value of type [String].
     *
     * Strings are stored in UTF-8 encoding.
     *
     * @param storage The storage to use for persistence.
     */
    @JvmStatic
    fun storingStrings(storage: MerkleStorage): StoredMerklePatriciaTrie<String> =
      StoredMerklePatriciaTrie(storage, ::stringSerializer, ::stringDeserializer)

    /**
     * Create a trie with keys and values of type [String].
     *
     * Strings are stored in UTF-8 encoding.
     *
     * @param storage The storage to use for persistence.
     * @param rootHash The initial root has for the trie, which should be already present in `storage`.
     */
    @JvmStatic
    fun storingStrings(storage: MerkleStorage, rootHash: Bytes32): StoredMerklePatriciaTrie<String> =
      StoredMerklePatriciaTrie(storage, rootHash, ::stringSerializer, ::stringDeserializer)

    /**
     * Create a trie.
     *
     * @param storage The storage to use for persistence.
     * @param valueSerializer A function for serializing values to bytes.
     * @param valueDeserializer A function for deserializing values from bytes.
     * @param <V> The serialized type.
     * @return A new merkle trie.
     */
    @JvmStatic
    fun <V> create(
      storage: MerkleStorage,
      valueSerializer: Function<V, Bytes>,
      valueDeserializer: Function<Bytes, V>
    ): StoredMerklePatriciaTrie<V> {
      return StoredMerklePatriciaTrie(storage, valueSerializer::apply, valueDeserializer::apply)
    }

    /**
     * Create a trie.
     *
     * @param storage The storage to use for persistence.
     * @param rootHash The initial root has for the trie, which should be already present in `storage`.
     * @param valueSerializer A function for serializing values to bytes.
     * @param valueDeserializer A function for deserializing values from bytes.
     * @param <V> The serialized type.
     * @return A new merkle trie.
     */
    @JvmStatic
    fun <V> create(
      storage: MerkleStorage,
      rootHash: Bytes32,
      valueSerializer: Function<V, Bytes>,
      valueDeserializer: Function<Bytes, V>
    ): StoredMerklePatriciaTrie<V> {
      return StoredMerklePatriciaTrie(storage, rootHash, valueSerializer::apply, valueDeserializer::apply)
    }
  }

  private val getVisitor = GetVisitor<V>()
  private val removeVisitor = RemoveVisitor<V>()
  private val storage: MerkleStorage
  private val nodeFactory: StoredNodeFactory<V>
  private var root: Node<V>
  override val coroutineContext: CoroutineContext = Dispatchers.Default

  /**
   * Create a trie.
   *
   * @param storage The storage to use for persistence.
   * @param valueSerializer A function for serializing values to bytes.
   * @param valueDeserializer A function for deserializing values from bytes.
   */
  constructor(
    storage: MerkleStorage,
    valueSerializer: (V) -> Bytes,
    valueDeserializer: (Bytes) -> V
  ) : this(storage, EMPTY_TRIE_ROOT_HASH, valueSerializer, valueDeserializer)

  /**
   * Create a trie.
   *
   * @param storage The storage to use for persistence.
   * @param rootHash The initial root has for the trie, which should be already present in `storage`.
   * @param valueSerializer A function for serializing values to bytes.
   * @param valueDeserializer A function for deserializing values from bytes.
   */
  constructor(
    storage: MerkleStorage,
    rootHash: Bytes32,
    valueSerializer: (V) -> Bytes,
    valueDeserializer: (Bytes) -> V
  ) {
    this.storage = storage
    this.nodeFactory = StoredNodeFactory(storage, valueSerializer, valueDeserializer)

    this.root = if (rootHash == EMPTY_TRIE_ROOT_HASH) {
      NullNode.instance()
    } else {
      StoredNode(nodeFactory, rootHash)
    }
  }

  override suspend fun get(key: Bytes): V? = root.accept(getVisitor, bytesToPath(key)).value()

  override suspend fun put(key: Bytes, value: V?) {
    if (value == null) {
      return remove(key)
    }
    updateRoot(root.accept(PutVisitor(nodeFactory, value), bytesToPath(key)))
  }

  override suspend fun remove(key: Bytes) = updateRoot(root.accept(removeVisitor, bytesToPath(key)))

  override fun rootHash(): Bytes32 = root.hash()

  /**
   * Forces any cached trie nodes to be released, so they can be garbage collected.
   *
   * Note: nodes are already stored using [java.lang.ref.SoftReference]'s, so they will be released automatically
   * based on memory demands.
   */
  fun clearCache() {
    val currentRoot = root
    if (currentRoot is StoredNode<*>) {
      currentRoot.unload()
    }
  }

  private suspend fun updateRoot(newRoot: Node<V>) {
    this.root = if (newRoot is StoredNode<*>) {
      newRoot
    } else {
      storage.put(newRoot.hash(), newRoot.rlp())
      StoredNode(nodeFactory, newRoot)
    }
  }

  /**
   * @return A string representation of the object.
   */
  override fun toString(): String {
    return javaClass.simpleName + "[" + rootHash() + "]"
  }

  suspend fun collect(maxRecords: Int, collector: (V) -> Boolean): Set<V> {
    val collectVisitor = CollectVisitor(maxRecords, collector)
    root.accept(collectVisitor, Bytes.EMPTY)
    return collectVisitor.results
  }

  fun printAsString(toStringFn: (V) -> String = { it.toString() }) = root.toString(toStringFn)
}
