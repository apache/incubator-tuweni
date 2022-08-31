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
package org.apache.tuweni.eth;

import static java.util.Objects.requireNonNull;
import static org.apache.tuweni.crypto.Hash.keccak256;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.bytes.DelegatingBytes32;

/**
 * An Ethereum hash.
 */
public final class Hash extends DelegatingBytes32 {

  /**
   * Create a Hash from Bytes.
   *
   * <p>
   * The hash must be exactly 32 bytes.
   *
   * @param bytes The bytes for this hash.
   * @return A hash.
   * @throws IllegalArgumentException If {@code bytes.size() != 32}.
   */
  public static Hash fromBytes(Bytes bytes) {
    requireNonNull(bytes);
    if (bytes.size() != SIZE) {
      throw new IllegalArgumentException(String.format("Expected %s bytes but got %s", SIZE, bytes.size()));
    }
    return new Hash(Bytes32.wrap(bytes));
  }

  /**
   * Create a Hash from Bytes32.
   *
   * @param bytes The bytes for this hash.
   * @return A hash.
   */
  public static Hash fromBytes(Bytes32 bytes) {
    return new Hash(bytes);
  }

  private Hash(Bytes delegate) {
    super(delegate);
  }

  /**
   * Parse a hexadecimal string into a {@link Hash}.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
   *        less than 32 bytes, in which case the result is left padded with zeros.
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation or
   *         contains more than 32 bytes.
   */
  public static Hash fromHexString(String str) {
    return new Hash(Bytes32.fromHexStringLenient(str));
  }

  public static Hash hash(Bytes value) {
    return new Hash(keccak256(value));
  }
}
