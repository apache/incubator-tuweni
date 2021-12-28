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
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.trie.CompactEncoding.bytesToPath
import java.util.function.Function
import kotlin.coroutines.CoroutineContext
import kotlin.text.Charsets.UTF_8

internal fun bytes32Identity(b: Bytes32): Bytes32 = b
internal fun bytesIdentity(b: Bytes): Bytes = b
internal fun stringSerializer(s: String): Bytes = Bytes.wrap(s.toByteArray(UTF_8))
internal fun stringDeserializer(b: Bytes): String = String(b.toArrayUnsafe(), UTF_8)

/**
 * An in-memory [MerkleTrie].
 *
 * @param <V> The type of values stored by this trie.
 * @param valueSerializer A function for serializing values to bytes.
 * @constructor Creates an empty trie.
 */

class MerklePatriciaTrie<V> @JvmOverloads constructor(valueSerializer: (V) -> Bytes, override val coroutineContext: CoroutineContext = Dispatchers.Default) : MerkleTrie<Bytes, V> {

  companion object {
    /**
     * Create a trie with keys and values of type [Bytes].
     */
    @JvmStatic
    fun storingBytes(): MerklePatriciaTrie<Bytes> =
      MerklePatriciaTrie(::bytesIdentity)

    /**
     * Create a trie with value of type [String].
     *
     * Strings are stored in UTF-8 encoding.
     */
    @JvmStatic
    fun storingStrings(): MerklePatriciaTrie<String> =
      MerklePatriciaTrie(::stringSerializer)

    /**
     * Create a trie.
     *
     * @param valueSerializer A function for serializing values to bytes.
     * @param <V> The serialized type.
     * @return A new merkle trie.
     */
    @JvmStatic
    fun <V> create(valueSerializer: Function<V, Bytes>): MerklePatriciaTrie<V> =
      MerklePatriciaTrie(valueSerializer::apply)
  }

  private val getVisitor = GetVisitor<V>()
  private val removeVisitor = RemoveVisitor<V>()
  private val nodeFactory: DefaultNodeFactory<V> = DefaultNodeFactory(valueSerializer)
  private var root: Node<V> = NullNode.instance()

  override suspend fun get(key: Bytes): V? = root.accept(getVisitor, bytesToPath(key)).value()

  // This implementation does not suspend, so we can use the unconfined context
  override fun getAsync(key: Bytes): AsyncResult<V?> = runBlocking(Dispatchers.Unconfined) {
    AsyncResult.completed(get(key))
  }

  override suspend fun put(key: Bytes, value: V?) {
    if (value == null) {
      return remove(key)
    }
    this.root = root.accept(PutVisitor(nodeFactory, value), bytesToPath(key))
  }

  // This implementation does not suspend, so we can use the unconfined context
  override fun putAsync(key: Bytes, value: V?): AsyncCompletion = runBlocking {
    put(key, value)
    AsyncCompletion.completed()
  }

  override suspend fun remove(key: Bytes) {
    this.root = root.accept(removeVisitor, bytesToPath(key))
  }

  // This implementation does not suspend, so we can use the unconfined context
  override fun removeAsync(key: Bytes): AsyncCompletion = runBlocking {
    remove(key)
    AsyncCompletion.completed()
  }

  override fun rootHash(): Bytes32 = root.hash()

  /**
   * @return A string representation of the object.
   */
  override fun toString(): String = javaClass.simpleName + "[" + rootHash() + "]"
}
