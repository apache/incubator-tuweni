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
package net.consensys.cava.trie;

import static org.junit.jupiter.api.Assertions.*;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.junit.BouncyCastleExtension;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class StoredMerklePatriciaTrieJavaTest {

  private MerkleStorage merkleStorage;
  private StoredMerklePatriciaTrie<String> trie;

  @BeforeEach
  void setup() {
    Map<Bytes32, Bytes> storage = new HashMap<>();
    merkleStorage = new AsyncMerkleStorage() {
      @Override
      public @NotNull AsyncResult<Bytes> getAsync(@NotNull Bytes32 hash) {
        return AsyncResult.completed(storage.get(hash));
      }

      @Override
      public @NotNull AsyncCompletion putAsync(@NotNull Bytes32 hash, @NotNull Bytes content) {
        storage.put(hash, content);
        return AsyncCompletion.completed();
      }
    };

    trie = StoredMerklePatriciaTrie.storingStrings(merkleStorage);
  }

  @Test
  void testEmptyTreeReturnsEmpty() throws Exception {
    assertNull(trie.getAsync(Bytes.EMPTY).get());
  }

  @Test
  void testEmptyTreeHasKnownRootHash() {
    assertEquals("0x56E81F171BCC55A6FF8345E692C0F86E5B48E01B996CADC001622FB5E363B421", trie.rootHash().toString());
  }

  @Test
  void testDeletesEntryUpdateWithNull() throws Exception {
    final Bytes key = Bytes.of(1);

    trie.putAsync(key, "value1").join();
    trie.putAsync(key, null).join();
    assertNull(trie.getAsync(key).get());
  }

  @Test
  void testReplaceSingleValue() throws Exception {
    final Bytes key = Bytes.of(1);

    trie.putAsync(key, "value1").join();
    assertEquals("value1", trie.getAsync(key).get());

    trie.putAsync(key, "value2").join();
    assertEquals("value2", trie.getAsync(key).get());
  }

  @Test
  void testHashChangesWhenSingleValueReplaced() throws Exception {
    final Bytes key = Bytes.of(1);

    trie.putAsync(key, "value1").join();
    final Bytes32 hash1 = trie.rootHash();

    trie.putAsync(key, "value2").join();
    final Bytes32 hash2 = trie.rootHash();

    assertNotEquals(hash2, hash1);

    trie.putAsync(key, "value1").join();
    assertEquals(hash1, trie.rootHash());
  }

  @Test
  void testReadPastLeaf() throws Exception {
    final Bytes key1 = Bytes.of(1);
    final Bytes key2 = Bytes.of(1, 3);
    trie.putAsync(key1, "value").join();
    assertNull(trie.getAsync(key2).get());
  }

  @Test
  void testBranchValue() throws Exception {
    final Bytes key1 = Bytes.of(1);
    final Bytes key2 = Bytes.of(16);

    trie.putAsync(key1, "value1").join();
    trie.putAsync(key2, "value2").join();
    assertEquals("value1", trie.getAsync(key1).get());
    assertEquals("value2", trie.getAsync(key2).get());
  }

  @Test
  void testReadPastBranch() throws Exception {
    final Bytes key1 = Bytes.of(12);
    final Bytes key2 = Bytes.of(12, 54);
    final Bytes key3 = Bytes.of(3);

    trie.putAsync(key1, "value1").join();
    trie.putAsync(key2, "value2").join();
    assertNull(trie.getAsync(key3).get());
  }

  @Test
  void testBranchWithValue() throws Exception {
    final Bytes key1 = Bytes.of(5);
    final Bytes key2 = Bytes.EMPTY;

    trie.putAsync(key1, "value1").join();
    trie.putAsync(key2, "value2").join();
    assertEquals("value1", trie.getAsync(key1).get());
    assertEquals("value2", trie.getAsync(key2).get());
  }

  @Test
  void testExtendAndBranch() throws Exception {
    final Bytes key1 = Bytes.of(1, 5, 9);
    final Bytes key2 = Bytes.of(1, 5, 2);

    trie.putAsync(key1, "value1").join();
    trie.putAsync(key2, "value2").join();
    assertEquals("value1", trie.getAsync(key1).get());
    assertEquals("value2", trie.getAsync(key2).get());
    assertNull(trie.getAsync(Bytes.of(1, 4)).get());
  }

  @Test
  void testBranchFromTopOfExtend() throws Exception {
    final Bytes key1 = Bytes.of(0xFE, 1);
    final Bytes key2 = Bytes.of(0xFE, 2);
    final Bytes key3 = Bytes.of(0xE1, 1);

    trie.putAsync(key1, "value1").join();
    trie.putAsync(key2, "value2").join();
    trie.putAsync(key3, "value3").join();
    assertEquals("value1", trie.getAsync(key1).get());
    assertEquals("value2", trie.getAsync(key2).get());
    assertEquals("value3", trie.getAsync(key3).get());
    assertNull(trie.getAsync(Bytes.of(1, 4)).get());
    assertNull(trie.getAsync(Bytes.of(2, 4)).get());
    assertNull(trie.getAsync(Bytes.of(3)).get());
  }

  @Test
  void testSplitBranchExtension() throws Exception {
    final Bytes key1 = Bytes.of(1, 5, 9);
    final Bytes key2 = Bytes.of(1, 5, 2);
    final Bytes key3 = Bytes.of(1, 9, 1);

    trie.putAsync(key1, "value1").join();
    trie.putAsync(key2, "value2").join();
    trie.putAsync(key3, "value3").join();
    assertEquals("value1", trie.getAsync(key1).get());
    assertEquals("value2", trie.getAsync(key2).get());
    assertEquals("value3", trie.getAsync(key3).get());
  }

  @Test
  void testReplaceBranchChild() throws Exception {
    final Bytes key1 = Bytes.of(0);
    final Bytes key2 = Bytes.of(1);

    trie.putAsync(key1, "value1").join();
    trie.putAsync(key2, "value2").join();
    assertEquals("value1", trie.getAsync(key1).get());
    assertEquals("value2", trie.getAsync(key2).get());

    trie.putAsync(key1, "value3").join();
    assertEquals("value3", trie.getAsync(key1).get());
    assertEquals("value2", trie.getAsync(key2).get());
  }

  @Test
  void testInlineBranchInBranch() throws Exception {
    final Bytes key1 = Bytes.of(0);
    final Bytes key2 = Bytes.of(1);
    final Bytes key3 = Bytes.of(2);
    final Bytes key4 = Bytes.of(0, 0);
    final Bytes key5 = Bytes.of(0, 1);

    trie.putAsync(key1, "value1").join();
    trie.putAsync(key2, "value2").join();
    trie.putAsync(key3, "value3").join();
    trie.putAsync(key4, "value4").join();
    trie.putAsync(key5, "value5").join();

    trie.removeAsync(key2).join();
    trie.removeAsync(key3).join();

    assertEquals("value1", trie.getAsync(key1).get());
    assertNull(trie.getAsync(key2).get());
    assertNull(trie.getAsync(key3).get());
    assertEquals("value4", trie.getAsync(key4).get());
    assertEquals("value5", trie.getAsync(key5).get());
  }

  @Test
  void testRemoveNodeInBranchExtensionHasNoEffect() throws Exception {
    final Bytes key1 = Bytes.of(1, 5, 9);
    final Bytes key2 = Bytes.of(1, 5, 2);

    trie.putAsync(key1, "value1").join();
    trie.putAsync(key2, "value2").join();

    final Bytes hash = trie.rootHash();
    trie.removeAsync(Bytes.of(1, 4)).join();
    assertEquals(hash, trie.rootHash());
  }

  @Test
  void testHashChangesWhenValueChanged() throws Exception {
    final Bytes key1 = Bytes.of(1, 5, 8, 9);
    final Bytes key2 = Bytes.of(1, 6, 1, 2);
    final Bytes key3 = Bytes.of(1, 6, 1, 3);

    trie.putAsync(key1, "value1").join();
    final Bytes32 hash1 = trie.rootHash();

    trie.putAsync(key2, "value2").join();
    trie.putAsync(key3, "value3").join();
    final Bytes32 hash2 = trie.rootHash();

    assertNotEquals(hash2, hash1);

    trie.putAsync(key1, "value4").join();
    final Bytes32 hash3 = trie.rootHash();

    assertNotEquals(hash3, hash1);
    assertNotEquals(hash3, hash2);

    trie.clearCache();

    trie.putAsync(key1, "value1").join();
    assertEquals(hash2, trie.rootHash());

    trie.removeAsync(key2).join();
    trie.removeAsync(key3).join();
    assertEquals(hash1, trie.rootHash());
  }

  @Test
  void testCanReloadTrieFromHash() throws Exception {
    final Bytes key1 = Bytes.of(1, 5, 8, 9);
    final Bytes key2 = Bytes.of(1, 6, 1, 2);
    final Bytes key3 = Bytes.of(1, 6, 1, 3);

    trie.putAsync(key1, "value1").join();
    final Bytes32 hash1 = trie.rootHash();

    trie.putAsync(key2, "value2").join();
    trie.putAsync(key3, "value3").join();
    final Bytes32 hash2 = trie.rootHash();
    assertNotEquals(hash2, hash1);

    trie.putAsync(key1, "value4").join();
    final Bytes32 hash3 = trie.rootHash();
    assertNotEquals(hash3, hash1);
    assertNotEquals(hash3, hash2);

    assertEquals("value4", trie.getAsync(key1).get());

    trie = StoredMerklePatriciaTrie.storingStrings(merkleStorage, hash1);
    assertEquals("value1", trie.getAsync(key1).get());
    assertNull(trie.getAsync(key2).get());
    assertNull(trie.getAsync(key3).get());

    trie = StoredMerklePatriciaTrie.storingStrings(merkleStorage, hash2);
    assertEquals("value1", trie.getAsync(key1).get());
    assertEquals("value2", trie.getAsync(key2).get());
    assertEquals("value3", trie.getAsync(key3).get());

    trie = StoredMerklePatriciaTrie.storingStrings(merkleStorage, hash3);
    assertEquals("value4", trie.getAsync(key1).get());
    assertEquals("value2", trie.getAsync(key2).get());
    assertEquals("value3", trie.getAsync(key3).get());
  }
}
