/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.trie

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.bytes.Bytes32

internal interface Node<V> {

  suspend fun accept(visitor: NodeVisitor<V>, path: Bytes): Node<V>

  suspend fun path(): Bytes

  suspend fun value(): V?

  fun rlp(): Bytes

  fun rlpRef(): Bytes

  fun hash(): Bytes32

  suspend fun replacePath(path: Bytes): Node<V>
}
