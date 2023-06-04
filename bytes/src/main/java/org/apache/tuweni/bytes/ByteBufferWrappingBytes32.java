// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.apache.tuweni.bytes.Checks.checkArgument;

import java.nio.ByteBuffer;

class ByteBufferWrappingBytes32 extends ByteBufferWrappingBytes implements Bytes32 {

  ByteBufferWrappingBytes32(ByteBuffer byteBuffer) {
    this(byteBuffer, 0, byteBuffer.limit());
  }

  ByteBufferWrappingBytes32(ByteBuffer byteBuffer, int offset, int length) {
    super(byteBuffer, offset, length);
    checkArgument(length == SIZE, "Expected %s bytes but got %s", SIZE, length);
  }

  // MUST be overridden by mutable implementations
  @Override
  public Bytes32 copy() {
    if (offset == 0 && length == byteBuffer.limit()) {
      return this;
    }
    return new ArrayWrappingBytes32(toArray());
  }

  @Override
  public MutableBytes32 mutableCopy() {
    return new MutableArrayWrappingBytes32(toArray());
  }
}
