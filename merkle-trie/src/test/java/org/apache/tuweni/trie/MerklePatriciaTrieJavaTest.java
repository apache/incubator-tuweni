/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.trie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.junit.BouncyCastleExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class MerklePatriciaTrieJavaTest {
  private MerkleTrie<Bytes, String> trie;

  @BeforeEach
  void setup() {
    trie = MerklePatriciaTrie.storingStrings();
  }

  @Test
  void testEmptyTreeReturnsEmpty() throws Exception {
    assertNull(trie.getAsync(Bytes.EMPTY).get());
  }

  @Test
  void testEmptyTreeHasKnownRootHash() {
    assertEquals("0x56e81f171bcc55a6ff8345e692c0f86e5b48e01b996cadc001622fb5e363b421", trie.rootHash().toString());
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

    trie.putAsync(key1, "value1").join();
    assertEquals(hash2, trie.rootHash());

    trie.removeAsync(key2).join();
    trie.removeAsync(key3).join();
    assertEquals(hash1, trie.rootHash());
  }
}
