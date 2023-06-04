// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.io;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;

/** Utility methods for encoding and decoding base58 strings. */
public final class Base58 {
  private Base58() {}

  /**
   * Encode a byte array to a base58 encoded string.
   *
   * @param bytes The bytes to encode.
   * @return A base58 encoded string.
   */
  public static String encodeBytes(byte[] bytes) {
    requireNonNull(bytes);
    return new String(Base58Codec.encode(bytes), UTF_8);
  }

  /**
   * Encode bytes to a base58 encoded string.
   *
   * @param bytes The bytes to encode.
   * @return A base58 encoded string.
   */
  public static String encode(Bytes bytes) {
    requireNonNull(bytes);
    return encodeBytes(bytes.toArrayUnsafe());
  }

  /**
   * Decode a base58 encoded string to a byte array.
   *
   * @param b58 The base58 encoded string.
   * @return A byte array.
   */
  public static byte[] decodeBytes(String b58) {
    requireNonNull(b58);
    return Base58Codec.decode(b58);
  }

  /**
   * Decode a base58 encoded string to bytes.
   *
   * @param b58 The base58 encoded string.
   * @return The decoded bytes.
   */
  public static Bytes decode(String b58) {
    return Bytes.wrap(decodeBytes(b58));
  }
}
