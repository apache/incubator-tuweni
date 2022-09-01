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
package org.apache.tuweni.crypto.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.tuweni.bytes.Bytes;

import java.util.Arrays;
import javax.security.auth.Destroyable;

import jnr.ffi.Pointer;

/**
 * Key derivation.
 *
 * <p>
 * Multiple secret subkeys can be derived from a single master key.
 *
 * <p>
 * Given the master key and a key identifier, a subkey can be deterministically computed. However, given a subkey, an
 * attacker cannot compute the master key nor any other subkeys.
 */
public final class KeyDerivation {

  /**
   * Check if Sodium and key derivation support is available.
   *
   * <p>
   * Key derivation is supported in sodium native library version &gt;= 10.0.12.
   *
   * @return {@code true} if Sodium and key derivation support is available.
   */
  public static boolean isAvailable() {
    try {
      return Sodium.supportsVersion(Sodium.VERSION_10_0_12);
    } catch (UnsatisfiedLinkError e) {
      return false;
    }
  }

  private static void assertAvailable() {
    if (!isAvailable()) {
      throw new UnsupportedOperationException(
          "Sodium key derivation is not available (requires sodium native library version >= 10.0.12)");
    }
  }

  /**
   * A KeyDerivation master key.
   */
  public static final class MasterKey implements Destroyable {
    final Allocated value;

    private MasterKey(Pointer ptr, int length) {
      this.value = new Allocated(ptr, length);
    }

    @Override
    public void destroy() {
      value.destroy();
    }

    @Override
    public boolean isDestroyed() {
      return value.isDestroyed();
    }

    /**
     * Create a {@link MasterKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the key.
     * @return A key, based on the supplied bytes.
     */
    public static MasterKey fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link MasterKey} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the key.
     * @return A key, based on the supplied bytes.
     * @throws UnsupportedOperationException If key derivation support is not available.
     */
    public static MasterKey fromBytes(byte[] bytes) {
      assertAvailable();
      if (bytes.length != Sodium.crypto_kdf_keybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_kdf_keybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, MasterKey::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     * @throws UnsupportedOperationException If key derivation support is not available.
     */
    public static int length() {
      assertAvailable();
      long keybytes = Sodium.crypto_kdf_keybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_kdf_keybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    /**
     * Generate a new key using a random generator.
     *
     * @return A randomly generated key.
     * @throws UnsupportedOperationException If key derivation support is not available.
     */
    public static MasterKey random() {
      assertAvailable();
      int length = length();
      Pointer ptr = Sodium.malloc(length);
      try {
        // When support for 10.0.11 is dropped, use this instead
        //Sodium.crypto_kdf_keygen(ptr);
        Sodium.randombytes_buf(ptr, length);
        return new MasterKey(ptr, length);
      } catch (Throwable e) {
        Sodium.sodium_free(ptr);
        throw e;
      }
    }

    /**
     * Derive a sub key.
     *
     * @param length The length of the sub key, which must be between {@link #minSubKeyLength()} and
     *        {@link #maxSubKeyLength()}.
     * @param subkeyId The id for the sub key.
     * @param context The context for the sub key, which must be of length {@link #contextLength()}.
     * @return The derived sub key.
     */
    public Bytes deriveKey(int length, long subkeyId, byte[] context) {
      return Bytes.wrap(deriveKeyArray(length, subkeyId, context));
    }

    /**
     * Derive a sub key.
     *
     * @param length The length of the sub key, which must be between {@link #minSubKeyLength()} and
     *        {@link #maxSubKeyLength()}.
     * @param subkeyId The id for the sub key.
     * @param context The context for the sub key, which must be of length {@link #contextLength()}.
     * @return The derived sub key.
     */
    public byte[] deriveKeyArray(int length, long subkeyId, byte[] context) {
      if (value.isDestroyed()) {
        throw new IllegalStateException("MasterKey has been destroyed");
      }
      assertSubKeyLength(length);
      assertContextLength(context);

      byte[] subKey = new byte[length];
      int rc = Sodium.crypto_kdf_derive_from_key(subKey, subKey.length, subkeyId, context, value.pointer());
      if (rc != 0) {
        throw new SodiumException("crypto_kdf_derive_from_key: failed with result " + rc);
      }
      return subKey;
    }

    /**
     * Derive a sub key.
     *
     * @param length The length of the subkey.
     * @param subkeyId The id for the subkey.
     * @param context The context for the sub key, which must be of length &le; {@link #contextLength()}.
     * @return The derived sub key.
     */
    public Bytes deriveKey(int length, long subkeyId, String context) {
      return Bytes.wrap(deriveKeyArray(length, subkeyId, context));
    }

    /**
     * Derive a sub key.
     *
     * @param length The length of the subkey.
     * @param subkeyId The id for the subkey.
     * @param context The context for the sub key, which must be of length &le; {@link #contextLength()}.
     * @return The derived sub key.
     */
    public byte[] deriveKeyArray(int length, long subkeyId, String context) {
      int contextLen = contextLength();
      byte[] contextBytes = context.getBytes(UTF_8);
      if (context.length() > contextLen) {
        throw new IllegalArgumentException("context must be " + contextLen + " bytes, got " + context.length());
      }
      byte[] ctx;
      if (contextBytes.length == contextLen) {
        ctx = contextBytes;
      } else {
        ctx = Arrays.copyOf(contextBytes, contextLen);
      }

      return deriveKeyArray(length, subkeyId, ctx);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof MasterKey)) {
        return false;
      }
      MasterKey other = (MasterKey) obj;
      return other.value.equals(value);
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    /**
     * Obtain the bytes of this key.
     *
     * WARNING: This will cause the key to be copied into heap memory.
     *
     * @return The bytes of this key.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
     * Obtain the bytes of this key.
     *
     * WARNING: This will cause the key to be copied into heap memory. The returned array should be overwritten when no
     * longer required.
     *
     * @return The bytes of this key.
     */
    public byte[] bytesArray() {
      return value.bytesArray();
    }
  }

  /**
   * Provides the required length for the context
   * 
   * @return The required length for the context (8).
   */
  public static int contextLength() {
    long contextbytes = Sodium.crypto_kdf_contextbytes();
    if (contextbytes > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("crypto_kdf_bytes_min: " + contextbytes + " is too large");
    }
    return (int) contextbytes;
  }

  /**
   * Provides the minimum length for a new sub key
   * 
   * @return The minimum length for a new sub key (16).
   */
  public static int minSubKeyLength() {
    long length = Sodium.crypto_kdf_bytes_min();
    if (length > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("crypto_kdf_bytes_min: " + length + " is too large");
    }
    return (int) length;
  }

  /**
   * Provides the maximum length for a new sub key
   * 
   * @return The maximum length for a new sub key (64).
   */
  public static int maxSubKeyLength() {
    long length = Sodium.crypto_kdf_bytes_max();
    if (length > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("crypto_kdf_bytes_max: " + length + " is too large");
    }
    return (int) length;
  }

  private static void assertContextLength(byte[] context) {
    long contextBytes = Sodium.crypto_kdf_contextbytes();
    if (context.length != contextBytes) {
      throw new IllegalArgumentException("context must be " + contextBytes + " bytes, got " + context.length);
    }
  }

  private static void assertSubKeyLength(int length) {
    long minLength = Sodium.crypto_kdf_bytes_min();
    long maxLength = Sodium.crypto_kdf_bytes_max();
    if (length < minLength || length > maxLength) {
      throw new IllegalArgumentException("length is out of range [" + minLength + ", " + maxLength + "]");
    }
  }
}
