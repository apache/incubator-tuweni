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
import org.jetbrains.annotations.Nullable;

/**
 * Allocated objects track allocation of memory using Sodium.
 *
 * @see <a href="https://libsodium.gitbook.io/doc/memory_management">Secure memory</a>
 */
public final class Allocated implements Destroyable {

  /**
   * Assign bytes using Sodium memory allocation
   * 
   * @param bytes the bytes to assign
   * @return a new allocated value filled with the bytes
   */
  public static Allocated fromBytes(Bytes bytes) {
    Allocated allocated = Allocated.allocate(bytes.size());
    allocated.pointer().put(0, bytes.toArrayUnsafe(), 0, bytes.size());
    return allocated;
  }

  /**
   * Allocate bytes using Sodium memory allocation
   * 
   * @param length the length of the memory allocation, in bytes
   * @return a new allocated value
   */
  static Allocated allocate(long length) {
    Pointer ptr = Sodium.malloc(length);
    return new Allocated(ptr, (int) length);
  }

  @Nullable
  private Pointer ptr;
  private final int length;

  Allocated(Pointer ptr, int length) {
    this.ptr = ptr;
    this.length = length;
  }

  Pointer pointer() {
    if (isDestroyed()) {
      throw new IllegalArgumentException("SecretKey has been destroyed");
    }
    return ptr;
  }

  int length() {
    return length;
  }

  /**
   * Destroys the value from memory.
   */
  @Override
  public void destroy() {
    if (!isDestroyed()) {
      Pointer p = ptr;
      ptr = null;
      Sodium.sodium_free(p);
    }
  }

  /**
   * Returns true if the value is destroyed.
   *
   * @return true if the allocated value is destroyed
   */
  @Override
  public boolean isDestroyed() {
    return ptr == null;
  }

  /**
   * Provides the bytes of this key.
   * 
   * @return The bytes of this key.
   */
  public Bytes bytes() {
    return Bytes.wrap(bytesArray());
  }

  /**
   * Provides the bytes of this key.
   * 
   * @return The bytes of this key.
   */
  public byte[] bytesArray() {
    if (isDestroyed()) {
      throw new IllegalStateException("allocated value has been destroyed");
    }
    return Sodium.reify(ptr, length);
  }

  @Override
  protected void finalize() {
    Sodium.sodium_free(ptr);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Allocated)) {
      return false;
    }
    Allocated other = (Allocated) obj;
    if (isDestroyed()) {
      throw new IllegalStateException("allocated value has been destroyed");
    }
    if (other.isDestroyed()) {
      throw new IllegalStateException("allocated value has been destroyed");
    }
    return Sodium.sodium_memcmp(this.ptr, other.ptr, length) == 0;
  }

  @Override
  public int hashCode() {
    if (isDestroyed()) {
      throw new IllegalStateException("allocated value has been destroyed");
    }
    return Sodium.hashCode(ptr, length);
  }
}
