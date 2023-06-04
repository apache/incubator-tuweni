// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.apache.tuweni.bytes.Checks.checkArgument;
import static org.apache.tuweni.bytes.Checks.checkElementIndex;

import java.nio.ByteBuffer;

public class MutableByteBufferWrappingBytes extends ByteBufferWrappingBytes
    implements MutableBytes {

  MutableByteBufferWrappingBytes(ByteBuffer byteBuffer) {
    super(byteBuffer);
  }

  MutableByteBufferWrappingBytes(ByteBuffer byteBuffer, int offset, int length) {
    super(byteBuffer, offset, length);
  }

  @Override
  public void setInt(int i, int value) {
    byteBuffer.putInt(offset + i, value);
  }

  @Override
  public void setLong(int i, long value) {
    byteBuffer.putLong(offset + i, value);
  }

  @Override
  public void set(int i, byte b) {
    byteBuffer.put(offset + i, b);
  }

  @Override
  public void set(int i, Bytes b) {
    byte[] bytes = b.toArrayUnsafe();
    int byteIndex = 0;
    int thisIndex = offset + i;
    int end = bytes.length;
    while (byteIndex < end) {
      byteBuffer.put(thisIndex++, bytes[byteIndex++]);
    }
  }

  @Override
  public MutableBytes mutableSlice(int i, int length) {
    if (i == 0 && length == this.length) {
      return this;
    }
    if (length == 0) {
      return MutableBytes.EMPTY;
    }

    checkElementIndex(i, this.length);
    checkArgument(
        i + length <= this.length,
        "Provided length %s is too big: the value has size %s and has only %s bytes from %s",
        length,
        this.length,
        this.length - i,
        i);

    return new MutableByteBufferWrappingBytes(byteBuffer, offset + i, length);
  }

  @Override
  public Bytes copy() {
    return new ArrayWrappingBytes(toArray());
  }

  @Override
  public int hashCode() {
    return computeHashcode();
  }
}
