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
package org.apache.tuweni.io;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;

/**
 * Utility methods for encoding and decoding base32 strings.
 */
public final class Base32 {
  private static final org.apache.commons.codec.binary.Base32 codec = new org.apache.commons.codec.binary.Base32();


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
