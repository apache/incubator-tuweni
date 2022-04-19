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
package org.apache.tuweni.crypto;

import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.sodium.SHA256Hash;
import org.apache.tuweni.crypto.sodium.Sodium;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Various utilities for providing hashes (digests) of arbitrary data.
 *
 * Requires the BouncyCastleProvider to be loaded and available. See
 * https://www.bouncycastle.org/wiki/display/JA1/Provider+Installation for detail.
 */
public final class Hash {
  static boolean USE_SODIUM = Boolean.parseBoolean(System.getProperty("org.apache.tuweni.crypto.useSodium", "true"));

  private Hash() {}

  // SHA-2
  private static final String SHA2_256 = "SHA-256";
  private static final String SHA2_512_256 = "SHA-512/256";

  // Keccak
  private static final String KECCAK_256 = "KECCAK-256";
  private static final String KECCAK_512 = "KECCAK-512";

  static final ThreadLocal<Map<String, MessageDigest>> cachedDigests = ThreadLocal.withInitial(ConcurrentHashMap::new);

  // SHA-3
  private static final String SHA3_256 = "SHA3-256";
  private static final String SHA3_512 = "SHA3-512";



  /**
   * Helper method to generate a digest using the provided algorithm.
   *
   * @param input The input bytes to produce the digest for.
   * @param alg The name of the digest algorithm to use.
   * @return A digest.
   * @throws NoSuchAlgorithmException If no Provider supports a MessageDigestSpi implementation for the specified
   *         algorithm.
   */
  public static byte[] digestUsingAlgorithm(byte[] input, String alg) throws NoSuchAlgorithmException {
    requireNonNull(input);
    requireNonNull(alg);
    try {
      MessageDigest digest = cachedDigests.get().computeIfAbsent(alg, (key) -> {
        try {
          return MessageDigest.getInstance(key);
        } catch (NoSuchAlgorithmException e) {
          throw new RuntimeException(e);
        }
      });
      digest.update(input);
      return digest.digest();
    } catch (RuntimeException e) {
      if (e.getCause() instanceof NoSuchAlgorithmException) {
        throw (NoSuchAlgorithmException) e.getCause();
      } else {
        throw e;
      }
    }
  }

  /**
   * Helper method to generate a digest using the provided algorithm.
   *
   * @param input The input bytes to produce the digest for.
   * @param alg The name of the digest algorithm to use.
   * @return A digest.
   * @throws NoSuchAlgorithmException If no Provider supports a MessageDigestSpi implementation for the specified
   *         algorithm.
   */
  public static Bytes digestUsingAlgorithm(Bytes input, String alg) throws NoSuchAlgorithmException {
    requireNonNull(input);
    return Bytes.wrap(digestUsingAlgorithm(input.toArrayUnsafe(), alg));
  }

  /**
   * Digest using SHA2-256.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static byte[] sha2_256(byte[] input) {
    if (isSodiumAvailable()) {
      SHA256Hash.Input shaInput = SHA256Hash.Input.fromBytes(input);
      try {
        SHA256Hash.Hash result = SHA256Hash.hash(shaInput);
        try {
          return SHA256Hash.hash(shaInput).bytesArray();
        } finally {
          result.destroy();
        }
      } finally {
        shaInput.destroy();
      }
    }
    try {
      return digestUsingAlgorithm(input, SHA2_256);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using SHA2-256.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static Bytes32 sha2_256(Bytes input) {
    if (isSodiumAvailable()) {
      SHA256Hash.Input shaInput = SHA256Hash.Input.fromBytes(input);
      try {
        SHA256Hash.Hash result = SHA256Hash.hash(shaInput);
        try {
          return (Bytes32) SHA256Hash.hash(shaInput).bytes();
        } finally {
          result.destroy();
        }
      } finally {
        shaInput.destroy();
      }
    }
    try {
      return (Bytes32) digestUsingAlgorithm(input, SHA2_256);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  private static boolean isSodiumAvailable() {
    if (!USE_SODIUM) {
      return false;
    }
    USE_SODIUM = Sodium.isAvailable();
    return USE_SODIUM;
  }

  /**
   * Digest using SHA2-512/256.
   *
   * @param input The value to encode.
   * @return A digest.
   */
  public static byte[] sha2_512_256(byte[] input) {
    try {
      return digestUsingAlgorithm(input, SHA2_512_256);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using SHA-512/256.
   *
   * @param input The value to encode.
   * @return A digest.
   */
  public static Bytes32 sha2_512_256(Bytes input) {
    try {
      return (Bytes32) digestUsingAlgorithm(input, SHA2_512_256);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using keccak-256.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static byte[] keccak256(byte[] input) {
    try {
      return digestUsingAlgorithm(input, KECCAK_256);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using keccak-256.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static Bytes32 keccak256(Bytes input) {
    try {
      return (Bytes32) digestUsingAlgorithm(input, KECCAK_256);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using keccak-512.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static byte[] keccak512(byte[] input) {
    try {
      return digestUsingAlgorithm(input, KECCAK_512);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using keccak-512.
   *
   * @param input The input bytes to produce the digest for.
   * @return A digest.
   */
  public static Bytes keccak512(Bytes input) {
    try {
      return digestUsingAlgorithm(input, KECCAK_512);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using SHA3-256.
   *
   * @param input The value to encode.
   * @return A digest.
   */
  public static byte[] sha3_256(byte[] input) {
    try {
      return digestUsingAlgorithm(input, SHA3_256);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using SHA3-256.
   *
   * @param input The value to encode.
   * @return A digest.
   */
  public static Bytes32 sha3_256(Bytes input) {
    try {
      return (Bytes32) digestUsingAlgorithm(input, SHA3_256);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using SHA3-512.
   *
   * @param input The value to encode.
   * @return A digest.
   */
  public static byte[] sha3_512(byte[] input) {
    try {
      return digestUsingAlgorithm(input, SHA3_512);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }

  /**
   * Digest using SHA3-512.
   *
   * @param input The value to encode.
   * @return A digest.
   */
  public static Bytes sha3_512(Bytes input) {
    try {
      return digestUsingAlgorithm(input, SHA3_512);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Algorithm should be available but was not", e);
    }
  }
}
