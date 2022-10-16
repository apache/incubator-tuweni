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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.rlp.RLP
import java.lang.ref.SoftReference
import java.util.concurrent.atomic.AtomicReference

internal class StoredNode<V> : Node<V>, CoroutineScope {
  private val nodeFactory: StoredNodeFactory<V>
  private val hash: Bytes32

  @Volatile
  private var loaded: SoftReference<Node<V>>? = null
  private val loader = AtomicReference<Deferred<Node<V>>>()

  constructor(nodeFactory: StoredNodeFactory<V>, hash: Bytes32) {
    this.nodeFactory = nodeFactory
    this.hash = hash
  }

  constructor(nodeFactory: StoredNodeFactory<V>, node: Node<V>) {
    this.nodeFactory = nodeFactory
    this.hash = node.hash()
    this.loaded = SoftReference(node)
  }

  override suspend fun accept(visitor: NodeVisitor<V>, path: Bytes): Node<V> {
    val node = load()
    val resultNode = node.accept(visitor, path)
    if (node === resultNode) {
      return this
    }
    return resultNode
  }

  override suspend fun path(): Bytes = load().path()

  override suspend fun value(): V? = load().value()

  // Getting the rlp representation is only needed when persisting a concrete node
  override fun rlp(): Bytes = throw UnsupportedOperationException()

  override fun rlpRef(): Bytes {
    val loadedNode = loaded?.get()
    if (loadedNode != null) {
      return loadedNode.rlpRef()
    }
    // If this node was stored, then it must have a rlp larger than a hash
    return RLP.encodeValue(hash)
  }

  override fun hash(): Bytes32 = hash

  override suspend fun replacePath(path: Bytes): Node<V> = load().replacePath(path)

  private suspend fun load(): Node<V> {
    val loadedNode = loaded?.get()
    if (loadedNode != null) {
      return loadedNode
    }

    val deferred: Deferred<Node<V>> = async(start = CoroutineStart.LAZY) {
      val node = nodeFactory.retrieve(hash)
      loaded = SoftReference(node)
      loader.set(null)
      node
    }

    while (!loader.compareAndSet(null, deferred)) {
      // already loading
      val prevDeferred = loader.get()
      if (prevDeferred != null) {
        return prevDeferred.await()
      }
    }

    // we've set the loader

    // check for a loaded node again, in case a loader just completed
    val node = loaded?.get()
    if (node != null) {
      // remove our loader, if it's still set
      loader.compareAndSet(deferred, null)
      return node
    }

    return deferred.await()
  }

  fun unload() {
    val deferred: Deferred<Node<V>>? = loader.get()
    deferred?.cancel()
    loaded = null
  }

  override val coroutineContext = Dispatchers.IO

  override fun toString(toStringFn: (V) -> String): String = runBlocking {
    load().toString(toStringFn)
  }
}
