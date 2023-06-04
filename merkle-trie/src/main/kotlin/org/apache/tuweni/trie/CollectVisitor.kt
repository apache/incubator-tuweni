// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.trie

import org.apache.tuweni.bytes.Bytes

internal class CollectVisitor<V>(val maxRecords: Int, val selector: (V) -> Boolean) : NodeVisitor<V> {

  val results = mutableSetOf<V>()

  override suspend fun visit(extensionNode: ExtensionNode<V>, path: Bytes): Node<V> {
    return extensionNode.child().accept(this, path)
  }

  override suspend fun visit(branchNode: BranchNode<V>, path: Bytes): Node<V> {
    branchNode.value()?.let {
      if (selector(it)) {
        results.add(it)
      }
    }

    if (results.size >= maxRecords) {
      return NullNode.instance()
    }

    for (byteIndex in 0..15) {
      branchNode.child(byteIndex.toByte()).accept(this, path)
      if (results.size >= maxRecords) {
        return NullNode.instance()
      }
    }

    return NullNode.instance()
  }

  override suspend fun visit(leafNode: LeafNode<V>, path: Bytes): Node<V> {
    leafNode.value()?.let {
      if (selector(it)) {
        results.add(it)
      }
    }
    return NullNode.instance()
  }

  override suspend fun visit(nullNode: NullNode<V>, path: Bytes): Node<V> =
    NullNode.instance()
}
