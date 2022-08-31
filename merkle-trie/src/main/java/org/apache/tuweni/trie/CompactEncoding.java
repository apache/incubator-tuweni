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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.MutableBytes;

/**
 * Compact (Hex-prefix) encoding and decoding.
 *
 * <p>
 * An implementation of <a href=
 * "https://github.com/ethereum/wiki/wiki/Patricia-Tree#specification-compact-encoding-of-hex-sequence-with-optional-terminator">Compact
 * (Hex-prefix) encoding</a>.
 */
public final class CompactEncoding {
  private CompactEncoding() {}

  public static final byte LEAF_TERMINATOR = 0x10;

  /**
   * Calculate a RADIX-16 path for a given byte sequence.
   *
   * @param bytes The byte sequence to calculate the path for.
   * @return The Radix-16 path.
   */
  public static Bytes bytesToPath(Bytes bytes) {
    MutableBytes path = MutableBytes.create(bytes.size() * 2 + 1);
    int j = 0;
    for (int i = 0; i < bytes.size(); i += 1, j += 2) {
      byte b = bytes.get(i);
      path.set(j, (byte) ((b >>> 4) & 0x0f));
      path.set(j + 1, (byte) (b & 0x0f));
    }
    path.set(j, LEAF_TERMINATOR);
    return path;
  }

  /**
   * Encode a Radix-16 path.
   *
   * @param path A Radix-16 path.
   * @return A compact-encoded path.
   */
  public static Bytes encode(Bytes path) {
    int size = path.size();
    boolean isLeaf = size > 0 && path.get(size - 1) == LEAF_TERMINATOR;
    if (isLeaf) {
      size = size - 1;
    }

    MutableBytes encoded = MutableBytes.create((size + 2) / 2);
    int i = 0;
    int j = 0;

    if (size % 2 == 1) {
      // add first nibble to magic
      byte high = (byte) (isLeaf ? 0x03 : 0x01);
      byte low = path.get(i++);
      if ((low & 0xf0) != 0) {
        throw new IllegalArgumentException("Invalid path: contains elements larger than a nibble");
      }
      encoded.set(j++, (byte) (high << 4 | low));
    } else {
      byte high = (byte) (isLeaf ? 0x02 : 0x00);
      encoded.set(j++, (byte) (high << 4));
    }

    while (i < size) {
      byte high = path.get(i++);
      byte low = path.get(i++);
      if ((high & 0xf0) != 0 || (low & 0xf0) != 0) {
        throw new IllegalArgumentException("Invalid path: contains elements larger than a nibble");
      }
      encoded.set(j++, (byte) (high << 4 | low));
    }

    return encoded;
  }

  /**
   * Decode a compact-encoded path to Radix-16.
   *
   * @param encoded A compact-encoded path.
   * @return A Radix-16 path.
   */
  public static Bytes decode(Bytes encoded) {
    int size = encoded.size();
    if (size == 0) {
      throw new IllegalArgumentException("empty encoded");
    }
    byte magic = encoded.get(0);
    if ((magic & 0xc0) != 0) {
      throw new IllegalArgumentException("Invalid compact encoding");
    }

    boolean isLeaf = (magic & 0x20) != 0;

    int pathLength = ((size - 1) * 2) + (isLeaf ? 1 : 0);
    MutableBytes path;
    int i = 0;

    if ((magic & 0x10) != 0) {
      // need to use lower nibble of magic
      path = MutableBytes.create(pathLength + 1);
      path.set(i++, (byte) (magic & 0x0f));
    } else {
      path = MutableBytes.create(pathLength);
    }

    for (int j = 1; j < size; j++) {
      byte b = encoded.get(j);
      path.set(i++, (byte) ((b >>> 4) & 0x0f));
      path.set(i++, (byte) (b & 0x0f));
    }

    if (isLeaf) {
      path.set(i, LEAF_TERMINATOR);
    }

    return path;
  }
}
