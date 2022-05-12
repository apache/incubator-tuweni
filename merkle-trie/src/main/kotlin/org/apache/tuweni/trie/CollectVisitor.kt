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
