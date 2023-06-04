// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

final class MutableArrayWrappingBytes32 extends MutableArrayWrappingBytes implements MutableBytes32 {

  MutableArrayWrappingBytes32(byte[] bytes) {
    this(bytes, 0);
  }

  MutableArrayWrappingBytes32(byte[] bytes, int offset) {
    super(bytes, offset, SIZE);
  }

  @Override
  public Bytes32 copy() {
    return new ArrayWrappingBytes32(toArray());
  }

  @Override
  public MutableBytes32 mutableCopy() {
    return new MutableArrayWrappingBytes32(toArray());
  }
}
