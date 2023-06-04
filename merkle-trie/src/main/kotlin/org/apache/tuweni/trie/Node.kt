// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.trie

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32

internal interface Node<V> {

  suspend fun accept(visitor: NodeVisitor<V>, path: Bytes): Node<V>

  suspend fun path(): Bytes

  suspend fun value(): V?

  fun rlp(): Bytes

  fun rlpRef(): Bytes

  fun hash(): Bytes32

  suspend fun replacePath(path: Bytes): Node<V>

  fun toString(toStringFn: (V) -> String): String
}
