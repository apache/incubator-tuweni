// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.apache.tuweni.bytes.Checks.checkArgument;
import static org.apache.tuweni.bytes.Checks.checkElementIndex;

import io.vertx.core.buffer.Buffer;

final class MutableBufferWrappingBytes extends BufferWrappingBytes implements MutableBytes {

  MutableBufferWrappingBytes(Buffer buffer) {
    super(buffer);
  }

  MutableBufferWrappingBytes(Buffer buffer, int offset, int length) {
    super(buffer, offset, length);
  }

  @Override
  public void set(int i, byte b) {
    buffer.setByte(i, b);
  }

  @Override
  public void set(int i, Bytes b) {
    byte[] bytes = b.toArrayUnsafe();
    buffer.setBytes(i, bytes);
  }

  @Override
  public void setInt(int i, int value) {
    buffer.setInt(i, value);
  }

  @Override
  public void setLong(int i, long value) {
    buffer.setLong(i, value);
  }

  @Override
  public MutableBytes mutableSlice(int i, int length) {
    int size = size();
    if (i == 0 && length == size) {
      return this;
    }
    if (length == 0) {
      return MutableBytes.EMPTY;
    }

    checkElementIndex(i, size);
    checkArgument(
        i + length <= size,
        "Provided length %s is too big: the value has size %s and has only %s bytes from %s",
        length,
        size,
        size - i,
        i);

    return new MutableBufferWrappingBytes(buffer.slice(i, i + length));
  }

  @Override
  public Bytes copy() {
    return Bytes.wrap(toArray());
  }

  @Override
  public MutableBytes mutableCopy() {
    return MutableBytes.wrap(toArray());
  }

  @Override
  public int hashCode() {
    return computeHashcode();
  }
}
