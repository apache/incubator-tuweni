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
 * Generic hashing utility (BLAKE2b).
 *
 * @see <a href="https://libsodium.gitbook.io/doc/hashing/generic_hashing">Generic hashing</a>
 *
 */
public final class GenericHash {

  /**
   * Input of generic hash function.
   */
  public static final class Input implements Destroyable {
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

    /**
     * Create a {@link GenericHash.Input} from a pointer.
     *
     * @param allocated the allocated pointer
     * @return An input.
     */
    public static Input fromPointer(Allocated allocated) {
      return new Input(Sodium.dup(allocated.pointer(), allocated.length()), allocated.length());
    }

    /**
     * Create a {@link GenericHash.Input} from a hash.
     *
     * @param hash the hash
     * @return An input.
     */
    public static Input fromHash(Hash hash) {
      return new Input(Sodium.dup(hash.value.pointer(), hash.value.length()), hash.value.length());
    }

    /**
     * Create a {@link GenericHash.Input} from an array of bytes.
     *
     * @param bytes The bytes for the input.
     * @return An input.
     */
    public static Input fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link GenericHash.Input} from an array of bytes.
     *
     * @param bytes The bytes for the input.
     * @return An input.
     */
    public static Input fromBytes(byte[] bytes) {
      return Sodium.dup(bytes, GenericHash.Input::new);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof GenericHash.Input)) {
        return false;
      }
      Input other = (Input) obj;
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
   * Key of generic hash function.
   */
  public static final class Key implements Destroyable {
    private final Allocated value;

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
     *
     * Provides the length of the key
     * 
     * @return the length of the key
     */
    public int length() {
      return value.length();
    }

    /**
     * Create a {@link GenericHash.Key} from a pointer.
     *
     * @param allocated the allocated pointer
     * @return A key.
     */
    public static Key fromPointer(Allocated allocated) {
      return new Key(Sodium.dup(allocated.pointer(), allocated.length()), allocated.length());
    }

    /**
     * Create a {@link GenericHash.Key} from a hash.
     *
     * @param hash the hash
     * @return A key.
     */
    public static Key fromHash(Hash hash) {
      return new Key(Sodium.dup(hash.value.pointer(), hash.value.length()), hash.value.length());
    }

    /**
     * Create a {@link GenericHash.Key} from an array of bytes.
     *
     * @param bytes The bytes for the key.
     * @return A key.
     */
    public static Key fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link GenericHash.Key} from an array of bytes.
     *
     * @param bytes The bytes for the key.
     * @return A key.
     */
    public static Key fromBytes(byte[] bytes) {
      return Sodium.dup(bytes, GenericHash.Key::new);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof GenericHash.Key)) {
        return false;
      }
      Key other = (Key) obj;
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
   * Generic hash function output.
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
      if (!(obj instanceof GenericHash.Hash)) {
        return false;
      }
      GenericHash.Hash other = (GenericHash.Hash) obj;
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
     * Provide the length of this hash.
     *
     * @return the length of this hash.
     */
    public int length() {
      return value.length();
    }
  }

  /**
   * Creates a generic hash of specified length of the input
   * 
   * @param hashLength the length of the hash
   * @param input the input of the hash function
   * @return the hash of the input
   */
  public static Hash hash(int hashLength, Input input) {
    Pointer output = Sodium.malloc(hashLength);
    Sodium.crypto_generichash(output, hashLength, input.value.pointer(), input.length(), null, 0);
    return new Hash(output, hashLength);
  }

  /**
   * Creates a generic hash of specified length of the input
   *
   * @param hashLength the length of the hash
   * @param input the input of the hash function
   * @param key the key of the hash function
   * @return the hash of the input
   */
  public static Hash hash(int hashLength, Input input, Key key) {
    Pointer output = Sodium.malloc(hashLength);
    Sodium
        .crypto_generichash(
            output,
            hashLength,
            input.value.pointer(),
            input.length(),
            key.value.pointer(),
            key.length());
    return new Hash(output, hashLength);
  }
}
