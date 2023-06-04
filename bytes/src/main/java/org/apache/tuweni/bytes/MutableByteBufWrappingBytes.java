// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.apache.tuweni.bytes.Checks.checkArgument;
import static org.apache.tuweni.bytes.Checks.checkElementIndex;

import io.netty.buffer.ByteBuf;

final class MutableByteBufWrappingBytes extends ByteBufWrappingBytes implements MutableBytes {

  MutableByteBufWrappingBytes(ByteBuf buffer) {
    super(buffer);
  }

  MutableByteBufWrappingBytes(ByteBuf buffer, int offset, int length) {
    super(buffer, offset, length);
  }

  @Override
  public void clear() {
    byteBuf.setZero(0, byteBuf.capacity());
  }

  @Override
  public void set(int i, byte b) {
    byteBuf.setByte(i, b);
  }

  @Override
  public void set(int i, Bytes b) {
    byte[] bytes = b.toArrayUnsafe();
    byteBuf.setBytes(i, bytes);
  }

  @Override
  public void setInt(int i, int value) {
    byteBuf.setInt(i, value);
  }

  @Override
  public void setLong(int i, long value) {
    byteBuf.setLong(i, value);
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

    return new MutableByteBufWrappingBytes(byteBuf.slice(i, length));
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
