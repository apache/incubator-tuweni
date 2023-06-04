// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

final class MutableArrayWrappingBytes48 extends MutableArrayWrappingBytes
    implements MutableBytes48 {

  MutableArrayWrappingBytes48(byte[] bytes) {
    this(bytes, 0);
  }

  MutableArrayWrappingBytes48(byte[] bytes, int offset) {
    super(bytes, offset, SIZE);
  }

  @Override
  public Bytes48 copy() {
    return new ArrayWrappingBytes48(toArray());
  }

  @Override
  public MutableBytes48 mutableCopy() {
    return new MutableArrayWrappingBytes48(toArray());
  }
}
