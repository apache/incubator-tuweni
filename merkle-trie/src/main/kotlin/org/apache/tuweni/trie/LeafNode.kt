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
import org.apache.tuweni.crypto.Hash.keccak256
import org.apache.tuweni.rlp.RLP
import java.lang.ref.WeakReference

internal class LeafNode<V>(
  private val path: Bytes,
  private val value: V,
  private val nodeFactory: NodeFactory<V>,
  private val valueSerializer: (V) -> Bytes
) : Node<V> {
  @Volatile
  private var rlp: WeakReference<Bytes>? = null

  @Volatile
  private var hash: Bytes32? = null

  override suspend fun accept(visitor: NodeVisitor<V>, path: Bytes): Node<V> = visitor.visit(this, path)

  override suspend fun path(): Bytes = path

  override suspend fun value(): V? = value

  override fun rlp(): Bytes {
    val prevEncoded = rlp?.get()
    if (prevEncoded != null) {
      return prevEncoded
    }

    val encoded = RLP.encodeList { writer ->
      writer.writeValue(CompactEncoding.encode(path))
      writer.writeValue(valueSerializer(value))
    }
    rlp = WeakReference(encoded)
    return encoded
  }

  override fun rlpRef(): Bytes {
    val rlp = rlp()
    return if (rlp.size() < 32) rlp else RLP.encodeValue(hash())
  }

  override fun hash(): Bytes32 {
    hash?.let { return it }
    val hashed = keccak256(rlp())
    hash = hashed
    return hashed
  }

  override suspend fun replacePath(path: Bytes): Node<V> = nodeFactory.createLeaf(path, value)

  override fun toString(toStringFn: (V) -> String): String {
    return """Leaf:
	Ref: ${rlpRef()}
	Path: ${CompactEncoding.encode(path)}
	Value: ${value?.let(toStringFn) ?: "empty"}"""
  }
}
