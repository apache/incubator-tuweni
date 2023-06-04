// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.sodium;

import java.util.ArrayList;
import java.util.List;

import jnr.ffi.Pointer;

/** Concatenate elements allocated to Sodium memory. */
public final class Concatenate {

  private final List<Allocated> values = new ArrayList<>();

  /**
   * Adds a hash to the elements to concatenate.
   *
   * @param hash a generic hash
   * @return the Concatenate instance
   */
  public Concatenate add(GenericHash.Hash hash) {
    values.add(hash.value);
    return this;
  }

  /**
   * Adds a hash to the elements to concatenate.
   *
   * @param hash a generic hash
   * @return the Concatenate instance
   */
  public Concatenate add(SHA256Hash.Hash hash) {
    values.add(hash.value);
    return this;
  }

  /**
   * Adds a HMAC key to the elements to concatenate.
   *
   * @param key a HMAC key
   * @return the Concatenate instance
   */
  public Concatenate add(HMACSHA512256.Key key) {
    values.add(key.value);
    return this;
  }

  /**
   * Adds a memory allocated value to the elements to concatenate.
   *
   * @param allocated a memory allocated value
   * @return the Concatenate instance
   */
  public Concatenate add(Allocated allocated) {
    values.add(allocated);
    return this;
  }

  /**
   * Adds a key to the elements to concatenate.
   *
   * @param key a Diffie-Helman key
   * @return the Concatenate instance
   */
  public Concatenate add(DiffieHelman.Secret key) {
    values.add(key.value);
    return this;
  }

  /**
   * Adds a public key to the elements to concatenate.
   *
   * @param key a public key
   * @return the Concatenate instance
   */
  public Concatenate add(Signature.PublicKey key) {
    values.add(key.value);
    return this;
  }

  /**
   * Adds a public key to the elements to concatenate.
   *
   * @param key a public key
   * @return the Concatenate instance
   */
  public Concatenate add(Box.PublicKey key) {
    values.add(key.value);
    return this;
  }

  /**
   * Adds a key to the elements to concatenate.
   *
   * @param key a secret key
   * @return the Concatenate instance
   */
  public Concatenate add(Box.SecretKey key) {
    values.add(key.value);
    return this;
  }

  /**
   * Adds a key to the elements to concatenate.
   *
   * @param key a secret key
   * @return the Concatenate instance
   */
  public Concatenate add(Signature.SecretKey key) {
    values.add(key.value);
    return this;
  }

  /**
   * Concatenates the values collected into a new safe memory allocation
   *
   * @return the result of the concatenation operation
   */
  @SuppressWarnings("unchecked")
  public Allocated concatenate() {
    int concatenatedLength = values.stream().mapToInt(v -> v.length()).sum();
    Pointer ptr = Sodium.malloc(concatenatedLength);
    try {
      int index = 0;
      for (Allocated value : values) {
        ptr.transferFrom(index, value.pointer(), 0, value.length());
        index += value.length();
      }
      return new Allocated(ptr, concatenatedLength);
    } catch (Throwable e) {
      Sodium.sodium_free(ptr);
      throw new RuntimeException(e);
    }
  }
}
