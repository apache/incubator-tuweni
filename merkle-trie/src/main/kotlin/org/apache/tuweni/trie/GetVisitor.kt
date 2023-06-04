// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.trie

import org.apache.tuweni.bytes.Bytes

internal class GetVisitor<V> : NodeVisitor<V> {

  override suspend fun visit(extensionNode: ExtensionNode<V>, path: Bytes): Node<V> {
    val extensionPath = extensionNode.path()
    val commonPathLength = extensionPath.commonPrefixLength(path)
    assert(commonPathLength < path.size()) { "Visiting path doesn't end with a non-matching terminator" }

    if (commonPathLength < extensionPath.size()) {
      // path diverges before the end of the extension, so it cannot match
      return NullNode.instance()
    }

    return extensionNode.child().accept(this, path.slice(commonPathLength))
  }

  override suspend fun visit(branchNode: BranchNode<V>, path: Bytes): Node<V> {
    assert(path.size() > 0) { "Visiting path doesn't end with a non-matching terminator" }

    val childIndex = path.get(0)
    if (childIndex == CompactEncoding.LEAF_TERMINATOR) {
      return branchNode
    }

    return branchNode.child(childIndex).accept(this, path.slice(1))
  }

  override suspend fun visit(leafNode: LeafNode<V>, path: Bytes): Node<V> {
    val leafPath = leafNode.path()

    if (leafPath.commonPrefixLength(path) != leafPath.size()) {
      return NullNode.instance()
    }

    return leafNode
  }

  override suspend fun visit(nullNode: NullNode<V>, path: Bytes): Node<V> =
    NullNode.instance()
}
