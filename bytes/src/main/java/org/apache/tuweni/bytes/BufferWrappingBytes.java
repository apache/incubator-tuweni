// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.apache.tuweni.bytes.Checks.checkArgument;
import static org.apache.tuweni.bytes.Checks.checkElementIndex;

import io.vertx.core.buffer.Buffer;

class BufferWrappingBytes extends AbstractBytes {

  protected final Buffer buffer;

  BufferWrappingBytes(Buffer buffer) {
    this.buffer = buffer;
  }

  BufferWrappingBytes(Buffer buffer, int offset, int length) {
    checkArgument(length >= 0, "Invalid negative length");
    int bufferLength = buffer.length();
    checkElementIndex(offset, bufferLength + 1);
    checkArgument(
        offset + length <= bufferLength,
        "Provided length %s is too big: the buffer has size %s and has only %s bytes from %s",
        length,
        bufferLength,
        bufferLength - offset,
        offset);

    if (offset == 0 && length == bufferLength) {
      this.buffer = buffer;
    } else {
      this.buffer = buffer.slice(offset, offset + length);
    }
  }

  @Override
  public int size() {
    return buffer.length();
  }

  @Override
  public byte get(int i) {
    return buffer.getByte(i);
  }

  @Override
  public int getInt(int i) {
    return buffer.getInt(i);
  }

  @Override
  public long getLong(int i) {
    return buffer.getLong(i);
  }

  @Override
  public Bytes slice(int i, int length) {
    int size = buffer.length();
    if (i == 0 && length == size) {
      return this;
    }
    if (length == 0) {
      return Bytes.EMPTY;
    }

    checkElementIndex(i, size);
    checkArgument(
        i + length <= size,
        "Provided length %s is too big: the value has size %s and has only %s bytes from %s",
        length,
        size,
        size - i,
        i);

    return new BufferWrappingBytes(buffer.slice(i, i + length));
  }

  // MUST be overridden by mutable implementations
  @Override
  public Bytes copy() {
    return Bytes.wrap(toArray());
  }

  @Override
  public MutableBytes mutableCopy() {
    return MutableBytes.wrap(toArray());
  }

  @Override
  public void appendTo(Buffer buffer) {
    buffer.appendBuffer(this.buffer);
  }

  @Override
  public byte[] toArray() {
    return buffer.getBytes();
  }
}
