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
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlp.RLPException
import org.apache.tuweni.rlp.RLPReader
import java.util.Collections

internal class StoredNodeFactory<V>(
  private val storage: MerkleStorage,
  private val valueSerializer: (V) -> Bytes,
  private val valueDeserializer: (Bytes) -> V
) : NodeFactory<V> {

  private val nullNode: NullNode<V> = NullNode.instance()

  override suspend fun createExtension(path: Bytes, child: Node<V>): Node<V> {
    return maybeStore(ExtensionNode(path, child, this))
  }

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
    return maybeStore(BranchNode(newChildren, value, this, valueSerializer))
  }

  override suspend fun createLeaf(path: Bytes, value: V): Node<V> {
    return maybeStore(LeafNode(path, value, this, valueSerializer))
  }

  private suspend fun maybeStore(node: Node<V>): Node<V> {
    val nodeRLP = node.rlp()
    if (nodeRLP.size() < 32) {
      return node
    }
    storage.put(node.hash(), node.rlp())
    return StoredNode(this, node)
  }

  internal suspend fun retrieve(hash: Bytes32): Node<V> {
    val bytes = storage.get(hash)
    if (bytes == null) {
      return NullNode.instance()
    }
    val node = decode(bytes) { "Invalid RLP value for hash $hash" }
    assert(hash == node.hash()) { "Node hash ${node.hash()} not equal to expected $hash" }
    return node
  }

  private fun decode(rlp: Bytes, errMessage: () -> String): Node<V> {
    try {
      return RLP.decode(rlp) { reader -> decode(reader, errMessage) }
    } catch (ex: RLPException) {
      throw MerkleStorageException(errMessage(), ex)
    }
  }

  private fun decode(nodeRLPs: RLPReader, errMessage: () -> String): Node<V> {
    return nodeRLPs.readList { listReader ->
      val remaining = listReader.remaining()
      when (remaining) {
        1 -> decodeNull(listReader, errMessage)
        2 -> {
          val encodedPath = listReader.readValue()
          val path: Bytes
          try {
            path = CompactEncoding.decode(encodedPath)
          } catch (e: IllegalArgumentException) {
            throw MerkleStorageException(errMessage() + ": invalid path " + encodedPath, e)
          }

          val size = path.size()
          if (size > 0 && path.get(size - 1) == CompactEncoding.LEAF_TERMINATOR) {
            decodeLeaf(path, listReader, errMessage)
          } else {
            decodeExtension(path, listReader, errMessage)
          }
        }
        BranchNode.RADIX + 1 -> decodeBranch(listReader, errMessage)
        else -> throw MerkleStorageException(errMessage() + ": invalid list size " + remaining)
      }
    }
  }

  private fun decodeExtension(path: Bytes, valueRlp: RLPReader, errMessage: () -> String): Node<V> {
    val child = if (valueRlp.nextIsList()) {
      decode(valueRlp, errMessage)
    } else {
      val childHash: Bytes32
      try {
        childHash = Bytes32.wrap(valueRlp.readValue())
      } catch (e: RLPException) {
        throw MerkleStorageException(errMessage() + ": invalid extension target")
      } catch (e: IllegalArgumentException) {
        throw MerkleStorageException(errMessage() + ": invalid extension target")
      }
      StoredNode(this, childHash)
    }
    return ExtensionNode(path, child, this)
  }

  private fun decodeBranch(nodeRLPs: RLPReader, errMessage: () -> String): BranchNode<V> {
    val children = ArrayList<Node<V>>(BranchNode.RADIX.toInt())
    for (i in 0 until BranchNode.RADIX) {
      val updatedChild = when {
        nodeRLPs.nextIsEmpty() -> {
          nodeRLPs.readValue()
          nullNode
        }
        nodeRLPs.nextIsList() -> {
          val child = decode(nodeRLPs, errMessage)
          StoredNode(this, child)
        }
        else -> {
          val childHash: Bytes32
          try {
            childHash = Bytes32.wrap(nodeRLPs.readValue())
          } catch (e: RLPException) {
            throw MerkleStorageException(errMessage() + ": invalid branch child " + i)
          } catch (e: IllegalArgumentException) {
            throw MerkleStorageException(errMessage() + ": invalid branch child " + i)
          }
          StoredNode(this, childHash)
        }
      }
      children.add(updatedChild)
    }

    val value = if (nodeRLPs.nextIsEmpty()) {
      nodeRLPs.readValue()
      null
    } else {
      decodeValue(nodeRLPs, errMessage)
    }

    return BranchNode(children, value, this, valueSerializer)
  }

  private fun decodeLeaf(path: Bytes, valueRlp: RLPReader, errMessage: () -> String): LeafNode<V> {
    var value: V
    if (!valueRlp.nextIsEmpty()) {
      value = decodeValue(valueRlp, errMessage)
    } else {
      value = valueDeserializer(Bytes.EMPTY)
    }
    return LeafNode(path, value, this, valueSerializer)
  }

  private fun decodeNull(nodeRLPs: RLPReader, errMessage: () -> String): NullNode<V> {
    if (!nodeRLPs.nextIsEmpty()) {
      throw MerkleStorageException(errMessage() + ": list size 1 but not null")
    }
    nodeRLPs.readValue()
    return nullNode
  }

  private fun decodeValue(valueRlp: RLPReader, errMessage: () -> String): V {
    val bytes: Bytes
    try {
      bytes = valueRlp.readValue()
    } catch (ex: RLPException) {
      throw MerkleStorageException(errMessage() + ": failed decoding value rlp " + valueRlp, ex)
    }
    return deserializeValue(errMessage, bytes)
  }

  private fun deserializeValue(errMessage: () -> String, bytes: Bytes): V {
    try {
      return valueDeserializer(bytes)
    } catch (ex: IllegalArgumentException) {
      throw MerkleStorageException(errMessage() + ": failed deserializing value " + bytes, ex)
    }
  }
}
