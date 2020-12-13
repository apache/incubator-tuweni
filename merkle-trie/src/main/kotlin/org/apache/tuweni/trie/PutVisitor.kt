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

internal class PutVisitor<V>(
  private val nodeFactory: NodeFactory<V>,
  private val value: V
) : NodeVisitor<V> {

  override suspend fun visit(extensionNode: ExtensionNode<V>, path: Bytes): Node<V> {
    val extensionPath = extensionNode.path()
    val commonPathLength = extensionPath.commonPrefixLength(path)
    assert(commonPathLength < path.size()) { "Visiting path doesn't end with a non-matching terminator" }

    if (commonPathLength == extensionPath.size()) {
      val child = extensionNode.child()
      val updatedChild = child.accept(this, path.slice(commonPathLength))
      return extensionNode.replaceChild(updatedChild)
    }

    // The path diverges before the end of the extension, so create a new branch

    val leafIndex = path.get(commonPathLength)
    val leafPath = path.slice(commonPathLength + 1)

    val extensionIndex = extensionPath.get(commonPathLength)
    val updatedExtension = extensionNode.replacePath(extensionPath.slice(commonPathLength + 1))
    val leaf = nodeFactory.createLeaf(leafPath, value)
    val branch = nodeFactory.createBranch(leafIndex, leaf, extensionIndex, updatedExtension)

    if (commonPathLength == 0) {
      return branch
    }
    return nodeFactory.createExtension(extensionPath.slice(0, commonPathLength), branch)
  }

  override suspend fun visit(branchNode: BranchNode<V>, path: Bytes): Node<V> {
    assert(path.size() > 0) { "Visiting path doesn't end with a non-matching terminator" }

    val childIndex = path.get(0)
    if (childIndex == CompactEncoding.LEAF_TERMINATOR) {
      return branchNode.replaceValue(value)
    }

    val updatedChild = branchNode.child(childIndex).accept(this, path.slice(1))
    return branchNode.replaceChild(childIndex, updatedChild)
  }

  override suspend fun visit(leafNode: LeafNode<V>, path: Bytes): Node<V> {
    val leafPath = leafNode.path()
    val commonPathLength = leafPath.commonPrefixLength(path)

    // Check if the current leaf node should be replaced
    if (commonPathLength == leafPath.size() && commonPathLength == path.size()) {
      return nodeFactory.createLeaf(leafPath, value)
    }

    assert(
      commonPathLength < leafPath.size() && commonPathLength < path.size(),
      { "Should not have consumed non-matching terminator" }
    )

    // The current leaf path must be split to accommodate the new value.

    val newLeafIndex = path.get(commonPathLength)
    val newLeafPath = path.slice(commonPathLength + 1)

    val updatedLeafIndex = leafPath.get(commonPathLength)
    val updatedLeaf = leafNode.replacePath(leafPath.slice(commonPathLength + 1))

    val leaf = nodeFactory.createLeaf(newLeafPath, value)
    val branch = nodeFactory.createBranch(updatedLeafIndex, updatedLeaf, newLeafIndex, leaf)

    if (commonPathLength == 0) {
      return branch
    }

    return nodeFactory.createExtension(leafPath.slice(0, commonPathLength), branch)
  }

  override suspend fun visit(nullNode: NullNode<V>, path: Bytes): Node<V> = nodeFactory.createLeaf(path, value)
}
