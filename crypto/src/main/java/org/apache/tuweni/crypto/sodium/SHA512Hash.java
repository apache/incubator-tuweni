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

import java.util.Objects;
import javax.security.auth.Destroyable;

import jnr.ffi.Pointer;

/**
 * SHA-512 hashing.
 *
 * The SHA-256 and SHA-512 functions are provided for interoperability with other applications. If you are looking for a
 * generic hash function and not specifically SHA-2, using crypto_generichash() (BLAKE2b) might be a better choice.
 * <p>
 * These functions are also not suitable for hashing passwords or deriving keys from passwords. Use one of the password
 * hashing APIs instead.
 * <p>
 * These functions are not keyed and are thus deterministic. In addition, the untruncated versions are vulnerable to
 * length extension attacks.
 * <p>
 *
 * @see <a href="https://libsodium.gitbook.io/doc/advanced/sha-2_hash_function">SHA-2</a>
 */
public class SHA512Hash {

  /**
   * Input of a SHA-512 hash function
   */
  public static final class Input implements Destroyable {
    /**
     * Create a hash input from a Diffie-Helman secret
     *
     * @param secret a Diffie-Helman secret
     * @return a hash input
     */
    public static SHA512Hash.Input fromSecret(DiffieHelman.Secret secret) {
      return new SHA512Hash.Input(
          Sodium.dup(secret.value.pointer(), DiffieHelman.Secret.length()),
          DiffieHelman.Secret.length());
    }

    /**
     * Create a {@link SHA512Hash.Input} from a pointer.
     *
     * @param allocated the allocated pointer
     * @return An input.
     */
    public static SHA512Hash.Input fromPointer(Allocated allocated) {
      return new SHA512Hash.Input(Sodium.dup(allocated.pointer(), allocated.length()), allocated.length());
    }

    /**
     * Create a {@link SHA512Hash.Input} from a hash.
     *
     * @param hash the hash
     * @return An input.
     */
    public static SHA512Hash.Input fromHash(SHA512Hash.Hash hash) {
      return new SHA512Hash.Input(Sodium.dup(hash.value.pointer(), hash.value.length()), hash.value.length());
    }

    /**
     * Create a {@link SHA512Hash.Input} from an array of bytes.
     *
     * @param bytes The bytes for the input.
     * @return An input.
     */
    public static SHA512Hash.Input fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link SHA512Hash.Input} from an array of bytes.
     *
     * @param bytes The bytes for the input.
     * @return An input.
     */
    public static SHA512Hash.Input fromBytes(byte[] bytes) {
      return Sodium.dup(bytes, SHA512Hash.Input::new);
    }

    private final Allocated value;

    private Input(Pointer ptr, int length) {
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
     * Provides the length of the input
     * 
     * @return the length of the input
     */
    public int length() {
      return value.length();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof SHA512Hash.Input)) {
        return false;
      }
      SHA512Hash.Input other = (SHA512Hash.Input) obj;
      return other.value.equals(value);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(value);
    }

    /**
     * Provides the bytes of this key
     * 
     * @return The bytes of this key.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
     * Provides the bytes of this key
     * 
     * @return The bytes of this key.
     */
    public byte[] bytesArray() {
      return value.bytesArray();
    }
  }

  /**
   * SHA-512 hash output
   */
  public static final class Hash implements Destroyable {
    Allocated value;

    Hash(Pointer ptr, int length) {
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

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof SHA512Hash.Hash)) {
        return false;
      }
      SHA512Hash.Hash other = (SHA512Hash.Hash) obj;
      return other.value.equals(value);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(value);
    }

    /**
     * Obtain the bytes of this hash.
     *
     * WARNING: This will cause the hash to be copied into heap memory.
     *
     * @return The bytes of this hash.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
     * Obtain the bytes of this hash.
     *
     * WARNING: This will cause the hash to be copied into heap memory. The returned array should be overwritten when no
     * longer required.
     *
     * @return The bytes of this hash.
     */
    public byte[] bytesArray() {
      return value.bytesArray();
    }

    /**
     * Obtain the length of the hash in bytes (32).
     *
     * @return The length of the hash in bytes (32).
     */
    public static int length() {
      long hashbytes = Sodium.crypto_hash_sha512_bytes();
      if (hashbytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_hash_sha512_bytes: " + hashbytes + " is too large");
      }
      return (int) hashbytes;
    }
  }

  /**
   * Hashes input to a SHA-512 hash
   * 
   * @param input the input of the hash function
   * @return a SHA-512 hash of the input
   */
  public static SHA512Hash.Hash hash(SHA512Hash.Input input) {
    Pointer output = Sodium.malloc(SHA512Hash.Hash.length());
    Sodium.crypto_hash_sha512(output, input.value.pointer(), input.length());
    return new SHA512Hash.Hash(output, SHA512Hash.Hash.length());
  }
}
