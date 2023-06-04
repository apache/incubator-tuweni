// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.io;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;

/** Utility methods for encoding and decoding base64 URL safe strings. */
public final class Base64URLSafe {
  private Base64URLSafe() {}

  /**
   * Encode a byte array to a base64 encoded string.
   *
   * @param bytes The bytes to encode.
   * @return A base64 encoded string.
   */
  public static String encodeBytes(byte[] bytes) {
    requireNonNull(bytes);
    return new String(java.util.Base64.getUrlEncoder().encode(bytes), UTF_8);
  }

  /**
   * Encode bytes to a base64 encoded string.
   *
   * @param bytes The bytes to encode.
   * @return A base64 encoded string.
   */
  public static String encode(Bytes bytes) {
    requireNonNull(bytes);
    return encodeBytes(bytes.toArrayUnsafe());
  }

  /**
   * Decode a base64 encoded string to a byte array.
   *
   * @param b64 The base64 encoded string.
   * @return A byte array.
   */
  public static byte[] decodeBytes(String b64) {
    requireNonNull(b64);
    return java.util.Base64.getUrlDecoder().decode(b64.getBytes(UTF_8));
  }

  /**
   * Decode a base64 encoded string to bytes.
   *
   * @param b64 The base64 encoded string.
   * @return The decoded bytes.
   */
  public static Bytes decode(String b64) {
    return Bytes.wrap(decodeBytes(b64));
  }
}
