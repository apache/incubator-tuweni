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
import org.apache.tuweni.bytes.MutableBytes
import org.apache.tuweni.crypto.Hash.keccak256
import org.apache.tuweni.rlp.RLP
import java.lang.ref.WeakReference

private val NULL_NODE: NullNode<*> = NullNode.instance<Any>()

internal class BranchNode<V>(
  private val children: List<Node<V>>,
  private val value: V?,
  private val nodeFactory: NodeFactory<V>,
  private val valueSerializer: (V) -> Bytes
) : Node<V> {

  companion object {
    const val RADIX = CompactEncoding.LEAF_TERMINATOR.toInt()
  }

  @Volatile
  private var rlp: WeakReference<Bytes>? = null

  @Volatile
  private var hash: Bytes32? = null

  init {
    assert(children.size == RADIX)
  }

  override suspend fun accept(visitor: NodeVisitor<V>, path: Bytes): Node<V> = visitor.visit(this, path)

  override suspend fun path(): Bytes = Bytes.EMPTY

  override suspend fun value(): V? = value

  fun child(index: Byte): Node<V> = children[index.toInt()]

  override fun rlp(): Bytes {
    val prevEncoded = rlp?.get()
    if (prevEncoded != null) {
      return prevEncoded
    }
    val encoded = RLP.encodeList { out ->
      for (i in 0 until RADIX) {
        out.writeRLP(children[i].rlpRef())
      }
      if (value != null) {
        out.writeValue(valueSerializer(value))
      } else {
        out.writeValue(Bytes.EMPTY)
      }
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

  override suspend fun replacePath(path: Bytes): Node<V> = nodeFactory.createExtension(path, this)

  suspend fun replaceChild(index: Byte, updatedChild: Node<V>): Node<V> {
    val newChildren = ArrayList(children)
    newChildren[index.toInt()] = updatedChild

    if (updatedChild === NULL_NODE) {
      if (value != null && !hasChildren()) {
        return nodeFactory.createLeaf(Bytes.of(index), value)
      } else if (value == null) {
        val flattened = maybeFlatten(newChildren)
        if (flattened != null) {
          return flattened
        }
      }
    }

    return nodeFactory.createBranch(newChildren, value)
  }

  suspend fun replaceValue(value: V): Node<V> = nodeFactory.createBranch(children, value)

  suspend fun removeValue(): Node<V> = maybeFlatten(children) ?: nodeFactory.createBranch(children, null)

  private fun hasChildren(): Boolean {
    for (child in children) {
      if (child !== NULL_NODE) {
        return true
      }
    }
    return false
  }

  override fun toString(): String {
    return toString { it.toString() }
  }

  override fun toString(toStringFn: (V) -> String): String {
    val builder = StringBuilder()
    builder.append("Branch:")
    builder.append("\n\tRef: ").append(rlpRef())
    for (i in 0 until RADIX) {
      val child = child(i.toByte())
      val nullNode = NullNode.instance<V>()
      if (child !== nullNode) {
        val branchLabel = "[" + Integer.toHexString(i) + "] "
        val childRep: String = child.toString(toStringFn).replace("\n\t", "\n\t\t")
        builder.append("\n\t").append(branchLabel).append(childRep)
      }
    }
    builder.append("\n\tValue: ")
      .append(value?.let { toStringFn(it) } ?: "empty")
    return builder.toString()
  }
}

private suspend fun <V> maybeFlatten(children: List<Node<V>>): Node<V>? {
  val onlyChildIndex = findOnlyChild(children)
  if (onlyChildIndex < 0) {
    return null
  }

  val onlyChild = children[onlyChildIndex]

  // replace the path of the only child and return it
  val onlyChildPath = onlyChild.path()
  val completePath = MutableBytes.create(1 + onlyChildPath.size())
  completePath.set(0, onlyChildIndex.toByte())
  onlyChildPath.copyTo(completePath, 1)
  return onlyChild.replacePath(completePath)
}

private fun <V> findOnlyChild(children: List<Node<V>>): Int {
  var onlyChildIndex = -1
  assert(children.size == BranchNode.RADIX.toInt())
  for (i in 0 until BranchNode.RADIX) {
    if (children[i] !== NULL_NODE) {
      if (onlyChildIndex >= 0) {
        return -1
      }
      onlyChildIndex = i
    }
  }
  return onlyChildIndex
}
