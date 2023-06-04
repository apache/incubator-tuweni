// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlp;

import static java.util.Objects.requireNonNull;
import static org.apache.tuweni.rlp.RLP.encodeByteArray;
import static org.apache.tuweni.rlp.RLP.encodeLength;
import static org.apache.tuweni.rlp.RLP.encodeNumber;

import org.apache.tuweni.bytes.Bytes;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.function.Consumer;

final class ByteBufferRLPWriter implements RLPWriter {

  private ByteBuffer buffer;

  ByteBufferRLPWriter(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @Override
  public void writeRLP(Bytes value) {
    buffer.put(value.toArrayUnsafe());
  }

  @Override
  public void writeValue(Bytes value) {
    encodeByteArray(value.toArrayUnsafe(), buffer::put);
  }

  @Override
  public void writeByteArray(byte[] value) {
    encodeByteArray(value, buffer::put);
  }

  @Override
  public void writeByte(byte value) {
    encodeByteArray(new byte[] {value}, buffer::put);
  }

  @Override
  public void writeLong(long value) {
    buffer.put(encodeNumber(value));
  }

  @Override
  public void writeList(Consumer<RLPWriter> fn) {
    requireNonNull(fn);
    AccumulatingRLPWriter listWriter = new AccumulatingRLPWriter();
    fn.accept(listWriter);
    writeEncodedValuesAsList(listWriter.values());
  }

  private void writeEncodedValuesAsList(Deque<byte[]> values) {
    int totalSize = 0;
    for (byte[] value : values) {
      try {
        totalSize = Math.addExact(totalSize, value.length);
      } catch (ArithmeticException e) {
        throw new BufferOverflowException();
      }
    }
    buffer.put(encodeLength(totalSize, 0xc0));
    values.forEach(bytes -> buffer.put(bytes));
  }
}
