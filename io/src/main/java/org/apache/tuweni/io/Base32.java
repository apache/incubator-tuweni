// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.io;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;

/** Utility methods for encoding and decoding base32 strings. */
public final class Base32 {
  private static final org.apache.commons.codec.binary.Base32 codec =
      new org.apache.commons.codec.binary.Base32();

  private Base32() {}

  /**
   * Encode a byte array to a base32 encoded string.
   *
   * @param bytes The bytes to encode.
   * @return A base32 encoded string.
   */
  public static String encodeBytes(byte[] bytes) {
    requireNonNull(bytes);
    return new String(codec.encode(bytes), UTF_8);
  }

  /**
   * Encode bytes to a base32 encoded string.
   *
   * @param bytes The bytes to encode.
   * @return A base32 encoded string.
   */
  public static String encode(Bytes bytes) {
    requireNonNull(bytes);
    return encodeBytes(bytes.toArrayUnsafe());
  }

  /**
   * Decode a base32 encoded string to a byte array.
   *
   * @param b32 The base32 encoded string.
   * @return A byte array.
   */
  public static byte[] decodeBytes(String b32) {
    requireNonNull(b32);
    return codec.decode(b32.getBytes(UTF_8));
  }

  /**
   * Decode a base32 encoded string to bytes.
   *
   * @param b32 The base32 encoded string.
   * @return The decoded bytes.
   */
  public static Bytes decode(String b32) {
    return Bytes.wrap(decodeBytes(b32));
  }
}
