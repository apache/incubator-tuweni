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

import org.apache.tuweni.bytes.Bytes;

import javax.security.auth.Destroyable;

import jnr.ffi.Pointer;

// Documentation copied under the ISC License, from
// https://github.com/jedisct1/libsodium-doc/blob/424b7480562c2e063bc8c52c452ef891621c8480/secret-key_cryptography/secret-key_authentication.md

/**
 * Secret-key authentication.
 *
 * <p>
 * These operations computes an authentication tag for a message and a secret key, and provides a way to verify that a
 * given tag is valid for a given message and a key.
 *
 * <p>
 * The function computing the tag is deterministic: the same (message, key) tuple will always produce the same output.
 *
 * <p>
 * However, even if the message is public, knowing the key is required in order to be able to compute a valid tag.
 * Therefore, the key should remain confidential. The tag, however, can be public.
 *
 * <p>
 * A typical use case is:
 *
 * <ul>
 * <li>{@code A} prepares a message, add an authentication tag, sends it to {@code B}</li>
 * <li>{@code A} doesn't store the message</li>
 * <li>Later on, {@code B} sends the message and the authentication tag to {@code A}</li>
 * <li>{@code A} uses the authentication tag to verify that it created this message.</li>
 * </ul>
 *
 * <p>
 * This operation does not encrypt the message. It only computes and verifies an authentication tag.
 */
public final class Auth {
  private Auth() {}

  /**
   * An Auth key.
   */
  public static final class Key implements Destroyable {
    final Allocated value;

    private Key(Pointer ptr, int length) {
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
     * Create a {@link Key} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the key.
     * @return A key, based on the supplied bytes.
     */
    public static Key fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link Key} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the key.
     * @return A key, based on the supplied bytes.
     */
    public static Key fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_auth_keybytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_auth_keybytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Key::new);
    }

    /**
     * Obtain the length of the key in bytes (32).
     *
     * @return The length of the key in bytes (32).
     */
    public static int length() {
      long keybytes = Sodium.crypto_auth_keybytes();
      if (keybytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_auth_keybytes: " + keybytes + " is too large");
      }
      return (int) keybytes;
    }

    /**
     * Generate a new key using a random generator.
     *
     * @return A randomly generated key.
     */
    public static Key random() {
      int length = length();
      Pointer ptr = Sodium.malloc(length);
      try {
        // When support for 10.0.11 is dropped, use this instead
        //Sodium.crypto_auth_keygen(ptr);
        Sodium.randombytes_buf(ptr, length);
        return new Key(ptr, length);
      } catch (Throwable e) {
        Sodium.sodium_free(ptr);
        throw e;
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Key)) {
        return false;
      }
      Key other = (Key) obj;
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
   * Create an authentication tag for a given input.
   *
   * @param input The input to generate an authentication tag for.
   * @param key A confidential key.
   * @return The authentication tag.
   */
  public static Bytes auth(Bytes input, Key key) {
    return Bytes.wrap(auth(input.toArrayUnsafe(), key));
  }

  /**
   * Create an authentication tag for a given input.
   *
   * @param input The input to generate an authentication tag for.
   * @param key A confidential key.
   * @return The authentication tag.
   */
  public static byte[] auth(byte[] input, Key key) {
    if (key.isDestroyed()) {
      throw new IllegalStateException("Key has been destroyed");
    }
    long abytes = Sodium.crypto_auth_bytes();
    if (abytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_auth_bytes: " + abytes + " is too large");
    }
    byte[] tag = new byte[(int) abytes];

    int rc = Sodium.crypto_auth(tag, input, input.length, key.value.pointer());
    if (rc != 0) {
      throw new SodiumException("crypto_auth_bytes: failed with result " + rc);
    }
    return tag;
  }

  /**
   * Verify an input using an authentication tag.
   *
   * @param tag The authentication tag for the input.
   * @param input The input.
   * @param key A confidential key that was used for tag creation.
   * @return {@code true} if the tag correction authenticates the input (using the specified key).
   */
  public static boolean verify(Bytes tag, Bytes input, Key key) {
    return verify(tag.toArrayUnsafe(), input.toArrayUnsafe(), key);
  }

  /**
   * Verify an input using an authentication tag.
   *
   * @param tag The authentication tag for the input.
   * @param input The input.
   * @param key A confidential key that was used for tag creation.
   * @return {@code true} if the tag correction authenticates the input (using the specified key).
   */
  public static boolean verify(byte[] tag, byte[] input, Key key) {
    if (key.isDestroyed()) {
      throw new IllegalStateException("Key has been destroyed");
    }
    long abytes = Sodium.crypto_auth_bytes();
    if (tag.length != abytes) {
      throw new IllegalArgumentException("tag must be " + abytes + " bytes, got " + tag.length);
    }
    int rc = Sodium.crypto_auth_verify(tag, input, input.length, key.value.pointer());
    return (rc == 0);
  }
}
