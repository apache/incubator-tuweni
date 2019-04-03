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
import java.util.Collections

internal class DefaultNodeFactory<V>(private val valueSerializer: (V) -> Bytes) :
  NodeFactory<V> {

  private val nullNode: NullNode<V> = NullNode.instance()

  override suspend fun createExtension(path: Bytes, child: Node<V>): Node<V> =
    ExtensionNode(path, child, this)

  override suspend fun createBranch(leftIndex: Byte, left: Node<V>, rightIndex: Byte, right: Node<V>): Node<V> {
    assert(leftIndex <= BranchNode.RADIX)
    assert(rightIndex <= BranchNode.RADIX)
    assert(leftIndex != rightIndex)

    val children: MutableList<Node<V>> = Collections.nCopies(BranchNode.RADIX, nullNode).toMutableList()
    return when {
      leftIndex.toInt() == BranchNode.RADIX -> {
        children[rightIndex.toInt()] = right
        createBranch(children, left.value())
      }
      rightIndex.toInt() == BranchNode.RADIX -> {
        children[leftIndex.toInt()] = left
        createBranch(children, right.value())
      }
      else -> {
        children[leftIndex.toInt()] = left
        children[rightIndex.toInt()] = right
        createBranch(children, null)
      }
    }
  }

  override suspend fun createBranch(newChildren: List<Node<V>>, value: V?): Node<V> {
    return BranchNode(newChildren, value, this, valueSerializer)
  }

  override suspend fun createLeaf(path: Bytes, value: V): Node<V> {
    return LeafNode(path, value, this, valueSerializer)
  }
}
