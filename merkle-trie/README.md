<!---
Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
License. You may obtain a copy of the License at
 *
http://www.apache.org/licenses/LICENSE-2.0
 *
Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
 --->
# Patricia Merkle Trie

| Status         |           |
|----------------|-----------|
| Stability      | [stable]  |
| Component Type | [library] |

This library introduces a [Patricia Merkle tree](https://en.wikipedia.org/wiki/Merkle_tree) [implementation](https://tuweni.apache.org/docs/org.apache.tuweni.trie/-merkle-patricia-trie/index.html).

It can be backed in memory or using a [key-value store](https://tuweni.apache.org/docs/org.apache.tuweni.kv/index.html).

The library offers a few methods to define a trie in memory quickly:

* [`MerklePatriaTrie.storingStrings()`](https://tuweni.apache.org/docs/org.apache.tuweni.trie/-merkle-patricia-trie/storing-strings.html) defines a trie using keys and values as Strings.
* [`MerklePatriaTrie.storingBytes()`](https://tuweni.apache.org/docs/org.apache.tuweni.trie/-merkle-patricia-trie/storing-bytes.html) defines a trie using keys and values as Bytes.

The same approach works with a stored trie:

* [`StoredMerklePatriciaTrie.storingStrings(storage: MerkleStorage)`](https://tuweni.apache.org/docs/org.apache.tuweni.trie/-stored-merkle-patricia-trie/storing-strings.html)
* [`StoredMerklePatriciaTrie.storingBytes(storage: MerkleStorage)`](https://tuweni.apache.org/docs/org.apache.tuweni.trie/-stored-merkle-patricia-trie/storing-bytes.html)

You will need to provide a storage in this case, which you will define by implementing [`MerkleStorage`](https://tuweni.apache.org/docs/org.apache.tuweni.trie/-merkle-storage/index.html).

Note in Java, you should use [`AsyncMerkleStorage`](https://tuweni.apache.org/docs/org.apache.tuweni.trie/-async-merkle-storage/index.html) instead to avoid dealing with coroutines.

An easy way to provide storage is to rely on a key-value store as defined in the [kv](https://tuweni.apache.org/docs/org.apache.tuweni.kv/index.html) library.

[stable]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#stable
[library]:https://github.com/apache/incubator-tuweni/tree/main/docs/index.md#library