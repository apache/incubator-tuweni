// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import java.util.Arrays;

/**
 * A Bytes value with just one constant value throughout. Ideal to avoid allocating large byte
 * arrays filled with the same byte.
 */
class ConstantBytes32Value extends AbstractBytes implements Bytes32 {

  private final byte value;

  public ConstantBytes32Value(byte b) {
    this.value = b;
  }

  @Override
  public int size() {
    return 32;
  }

  @Override
  public byte get(int i) {
    return this.value;
  }

  @Override
  public Bytes slice(int i, int length) {
    if (length == 32) {
      return this;
    }
    return new ConstantBytesValue(this.value, length);
  }

  @Override
  public Bytes32 copy() {
    return new ConstantBytes32Value(this.value);
  }

  @Override
  public MutableBytes32 mutableCopy() {
    byte[] mutable = new byte[32];
    Arrays.fill(mutable, this.value);
    return new MutableArrayWrappingBytes32(mutable);
  }
}
