// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.trie

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash.keccak256
import org.apache.tuweni.rlp.RLP
import java.lang.ref.WeakReference

internal class ExtensionNode<V>(
  private val path: Bytes,
  private val child: Node<V>,
  private val nodeFactory: NodeFactory<V>,
) : Node<V> {
  @Volatile
  private var rlp: WeakReference<Bytes>? = null

  @Volatile
  private var hash: Bytes32? = null

  init {
    assert(path.size() > 0)
    assert(path.get(path.size() - 1) != CompactEncoding.LEAF_TERMINATOR) { "Extension path ends in a leaf terminator" }
  }

  override suspend fun accept(visitor: NodeVisitor<V>, path: Bytes): Node<V> = visitor.visit(this, path)

  override suspend fun path(): Bytes = path

  override suspend fun value(): V? = throw UnsupportedOperationException()

  fun child(): Node<V> = child

  override fun rlp(): Bytes {
    val prevEncoded = rlp?.get()
    if (prevEncoded != null) {
      return prevEncoded
    }
    val encoded = RLP.encodeList { writer ->
      writer.writeValue(CompactEncoding.encode(path))
      writer.writeRLP(child.rlpRef())
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

  suspend fun replaceChild(updatedChild: Node<V>): Node<V> {
    // collapse this extension - if the child is a branch, it will create a new extension
    val childPath = updatedChild.path()
    return updatedChild.replacePath(Bytes.concatenate(path, childPath))
  }

  override suspend fun replacePath(path: Bytes): Node<V> {
    return if (path.size() == 0) child else nodeFactory.createExtension(path, child)
  }

  override fun toString(toStringFn: (V) -> String): String {
    val builder = StringBuilder()
    val childRep: String = child.toString(toStringFn).replace("\n\t", "\n\t\t")
    builder
      .append("Extension:")
      .append("\n\tRef: ")
      .append(rlpRef())
      .append("\n\tPath: ")
      .append(CompactEncoding.encode(path))
      .append("\n\t")
      .append(childRep)
    return builder.toString()
  }
}
