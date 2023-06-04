// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.trie

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash.keccak256
import org.apache.tuweni.rlp.RLP

internal class NullNode<V> private constructor() : Node<V> {

  companion object {
    private val RLP_NULL = RLP.encodeByteArray(ByteArray(0))
    private val HASH = keccak256(RLP_NULL)
    private val instance = NullNode<Any>()

    @Suppress("UNCHECKED_CAST")
    fun <V> instance(): NullNode<V> = instance as NullNode<V>
  }

  override suspend fun accept(visitor: NodeVisitor<V>, path: Bytes): Node<V> = visitor.visit(this, path)

  override suspend fun path(): Bytes = Bytes.EMPTY

  override suspend fun value(): V? = null

  override fun rlp(): Bytes = RLP_NULL

  override fun rlpRef(): Bytes = RLP_NULL

  override fun hash(): Bytes32 = HASH

  override suspend fun replacePath(path: Bytes): Node<V> = this

  override fun toString() = "[NULL]"

  override fun toString(toStringFn: (V) -> String) = "[NULL]"
}
