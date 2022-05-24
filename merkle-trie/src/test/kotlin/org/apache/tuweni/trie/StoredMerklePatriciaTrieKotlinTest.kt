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

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.junit.BouncyCastleExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
internal class StoredMerklePatriciaTrieKotlinTest {

  private lateinit var storage: MutableMap<Bytes32, Bytes>
  private val merkleStorage = object : MerkleStorage {
    override suspend fun get(hash: Bytes32): Bytes? = storage[hash]

    override suspend fun put(hash: Bytes32, content: Bytes) {
      storage[hash] = content
    }
  }
  private lateinit var trie: StoredMerklePatriciaTrie<String>

  @BeforeEach
  fun setup() {
    storage = mutableMapOf()
    trie = StoredMerklePatriciaTrie.storingStrings(merkleStorage)
  }

  @Test
  fun testEmptyTreeReturnsEmpty() {
    runBlocking {
      assertNull(trie.get(Bytes.EMPTY))
    }
  }

  @Test
  fun testEmptyTreeHasKnownRootHash() {
    assertEquals("0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421", trie.rootHash().toString())
  }

  @Test
  fun testDeletesEntryUpdateWithNull() {
    val key = Bytes.of(1)
    runBlocking {
      trie.put(key, "value1")
      trie.put(key, null)
      assertNull(trie.get(key))
    }
  }

  @Test
  fun testReplaceSingleValue() {
    val key = Bytes.of(1)
    val value1 = "value1"
    val value2 = "value2"
    runBlocking {
      trie.put(key, value1)
      assertEquals(value1, trie.get(key))

      trie.put(key, value2)
      assertEquals(value2, trie.get(key))
    }
  }

  @Test
  fun testHashChangesWhenSingleValueReplaced() {
    val key = Bytes.of(1)
    runBlocking {
      trie.put(key, "value1")
      val hash1 = trie.rootHash()

      trie.put(key, "value2")
      val hash2 = trie.rootHash()

      assertNotEquals(hash2, hash1)

      trie.put(key, "value1")
      assertEquals(hash1, trie.rootHash())
    }
  }

  @Test
  fun testReadPastLeaf() {
    val key1 = Bytes.of(1)
    val key2 = Bytes.of(1, 3)
    runBlocking {
      trie.put(key1, "value")
      assertNull(trie.get(key2))
    }
  }

  @Test
  fun testBranchValue() {
    val key1 = Bytes.of(1)
    val key2 = Bytes.of(16)
    runBlocking {
      trie.put(key1, "value1")
      trie.put(key2, "value2")
      assertEquals("value1", trie.get(key1))
      assertEquals("value2", trie.get(key2))
    }
  }

  @Test
  fun testReadPastBranch() {
    val key1 = Bytes.of(12)
    val key2 = Bytes.of(12, 54)
    val key3 = Bytes.of(3)
    runBlocking {
      trie.put(key1, "value1")
      trie.put(key2, "value2")
      assertNull(trie.get(key3))
    }
  }

  @Test
  fun testBranchWithValue() {
    val key1 = Bytes.of(5)
    val key2 = Bytes.EMPTY
    runBlocking {
      trie.put(key1, "value1")
      trie.put(key2, "value2")
      assertEquals("value1", trie.get(key1))
      assertEquals("value2", trie.get(key2))
    }
  }

  @Test
  fun testExtendAndBranch() {
    val key1 = Bytes.of(1, 5, 9)
    val key2 = Bytes.of(1, 5, 2)
    val key3 = Bytes.of(1, 4)
    runBlocking {
      trie.put(key1, "value1")
      trie.put(key2, "value2")
      assertEquals("value1", trie.get(key1))
      assertEquals("value2", trie.get(key2))
      assertNull(trie.get(key3))
    }
  }

  @Test
  fun testBranchFromTopOfExtend() {
    val key1 = Bytes.of(0xFE, 1)
    val key2 = Bytes.of(0xFE, 2)
    val key3 = Bytes.of(0xE1, 1)
    runBlocking {
      trie.put(key1, "value1")
      trie.put(key2, "value2")
      trie.put(key3, "value3")
      assertEquals("value1", trie.get(key1))
      assertEquals("value2", trie.get(key2))
      assertEquals("value3", trie.get(key3))
      assertNull(trie.get(Bytes.of(1, 4)))
      assertNull(trie.get(Bytes.of(2, 4)))
      assertNull(trie.get(Bytes.of(3)))
    }
  }

  @Test
  fun testSplitBranchExtension() {
    val key1 = Bytes.of(1, 5, 9)
    val key2 = Bytes.of(1, 5, 2)
    val key3 = Bytes.of(1, 9, 1)
    runBlocking {
      trie.put(key1, "value1")
      trie.put(key2, "value2")
      trie.put(key3, "value3")
      assertEquals("value1", trie.get(key1))
      assertEquals("value2", trie.get(key2))
      assertEquals("value3", trie.get(key3))
    }
  }

  @Test
  fun testReplaceBranchChild() {
    val key1 = Bytes.of(0)
    val key2 = Bytes.of(1)
    runBlocking {
      trie.put(key1, "value1")
      trie.put(key2, "value2")
      assertEquals("value1", trie.get(key1))
      assertEquals("value2", trie.get(key2))

      trie.put(key1, "value3")
      assertEquals("value3", trie.get(key1))
      assertEquals("value2", trie.get(key2))
    }
  }

  @Test
  fun testInlineBranchInBranch() {
    val key1 = Bytes.of(0)
    val key2 = Bytes.of(1)
    val key3 = Bytes.of(2)
    val key4 = Bytes.of(0, 0)
    val key5 = Bytes.of(0, 1)
    runBlocking {
      trie.put(key1, "value1")
      trie.put(key2, "value2")
      trie.put(key3, "value3")
      trie.put(key4, "value4")
      trie.put(key5, "value5")

      trie.remove(key2)
      trie.remove(key3)

      assertEquals("value1", trie.get(key1))
      assertNull(trie.get(key2))
      assertNull(trie.get(key3))
      assertEquals("value4", trie.get(key4))
      assertEquals("value5", trie.get(key5))
    }
  }

  @Test
  fun testRemoveNodeInBranchExtensionHasNoEffect() {
    val key1 = Bytes.of(1, 5, 9)
    val key2 = Bytes.of(1, 5, 2)
    runBlocking {
      trie.put(key1, "value1")
      trie.put(key2, "value2")

      val hash = trie.rootHash()
      trie.remove(Bytes.of(1, 4))
      assertEquals(hash, trie.rootHash())
    }
  }

  @Test
  fun testHashChangesWhenValueChanged() {
    val key1 = Bytes.of(1, 5, 8, 9)
    val key2 = Bytes.of(1, 6, 1, 2)
    val key3 = Bytes.of(1, 6, 1, 3)
    runBlocking {
      trie.put(key1, "value1")
      val hash1 = trie.rootHash()

      trie.put(key2, "value2")
      trie.put(key3, "value3")
      val hash2 = trie.rootHash()

      assertNotEquals(hash2, hash1)

      trie.put(key1, "value4")
      val hash3 = trie.rootHash()

      assertNotEquals(hash3, hash1)
      assertNotEquals(hash3, hash2)

      trie.clearCache()

      trie.put(key1, "value1")
      assertEquals(hash2, trie.rootHash())

      trie.remove(key2)
      trie.remove(key3)
      assertEquals(hash1, trie.rootHash())
    }
  }

  @Test
  fun testCanReloadTrieFromHash() {
    val key1 = Bytes.of(1, 5, 8, 9)
    val key2 = Bytes.of(1, 6, 1, 2)
    val key3 = Bytes.of(1, 6, 1, 3)
    runBlocking {
      trie.put(key1, "value1")
    }
    val hash1 = trie.rootHash()

    runBlocking {
      trie.put(key2, "value2")
      trie.put(key3, "value3")
    }
    val hash2 = trie.rootHash()
    assertNotEquals(hash2, hash1)

    runBlocking {
      trie.put(key1, "value4")
      assertEquals("value4", trie.get(key1))
    }
    val hash3 = trie.rootHash()
    assertNotEquals(hash3, hash1)
    assertNotEquals(hash3, hash2)

    val trie1 = StoredMerklePatriciaTrie.storingStrings(merkleStorage, hash1)
    runBlocking {
      assertEquals("value1", trie1.get(key1))
      assertNull(trie1.get(key2))
      assertNull(trie1.get(key3))
    }

    val trie2 = StoredMerklePatriciaTrie.storingStrings(merkleStorage, hash2)
    runBlocking {
      assertEquals("value1", trie2.get(key1))
      assertEquals("value2", trie2.get(key2))
      assertEquals("value3", trie2.get(key3))
    }

    val trie3 = StoredMerklePatriciaTrie.storingStrings(merkleStorage, hash3)
    runBlocking {
      assertEquals("value4", trie3.get(key1))
      assertEquals("value2", trie3.get(key2))
      assertEquals("value3", trie3.get(key3))
    }
  }

  @Test
  fun testRootHash() = runBlocking {
    val store = mutableMapOf<Bytes, Bytes>()
    val tree = StoredMerklePatriciaTrie.storingBytes(
      object : MerkleStorage {
        override suspend fun get(hash: Bytes32): Bytes? {
          return store.get(hash)
        }

        override suspend fun put(hash: Bytes32, content: Bytes) {
          store.put(hash, content)
        }
      },
      MerkleTrie.EMPTY_TRIE_ROOT_HASH
    )
    assertEquals(MerkleTrie.EMPTY_TRIE_ROOT_HASH, tree.rootHash())
    tree.put(
      Hash.keccak256(Bytes32.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001")),
      Bytes.fromHexString("0x32")
    )
    assertEquals(Bytes32.fromHexString("0x9ff0b4b6f084c8432d75c0549758a165ec9d80e5f01440d8753d57a9c95f529e"), tree.rootHash())
  }

  @Test
  fun testRootHashDifferentKey() = runBlocking {
    val store = mutableMapOf<Bytes, Bytes>()
    val tree = StoredMerklePatriciaTrie.storingBytes(
      object : MerkleStorage {
        override suspend fun get(hash: Bytes32): Bytes? {
          return store.get(hash)
        }

        override suspend fun put(hash: Bytes32, content: Bytes) {
          store.put(hash, content)
        }
      },
      MerkleTrie.EMPTY_TRIE_ROOT_HASH
    )
    assertEquals(MerkleTrie.EMPTY_TRIE_ROOT_HASH, tree.rootHash())
    tree.put(
      Hash.keccak256(Bytes32.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000001")),
      Bytes.fromHexString("0x81c8")
    )
    assertEquals(Bytes32.fromHexString("0x4e05e0192d92867cde657332e4d893cf4d1fbbfca597067a7a385b876efe0a21"), tree.rootHash())
  }
}
