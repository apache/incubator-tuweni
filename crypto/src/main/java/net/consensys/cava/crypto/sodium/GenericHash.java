/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.crypto.sodium;

import net.consensys.cava.bytes.Bytes;

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
     * @return The bytes of this key.
     */
    public Bytes bytes() {
      return value.bytes();
    }

    /**
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
     * @deprecated Use {@link #bytesArray()} to obtain the bytes as an array, which should be overwritten when no longer
     *             required.
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
}
