// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.rlp;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

import java.math.BigInteger;
import java.util.function.Consumer;

class DelegatingRLPWriter<T extends AccumulatingRLPWriter> implements RLPWriter {

  T delegate;

  DelegatingRLPWriter(T delegate) {
    this.delegate = delegate;
  }

  @Override
  public void writeRLP(Bytes value) {
    delegate.writeRLP(value);
  }

  @Override
  public void writeValue(Bytes value) {
    delegate.writeValue(value);
  }

  @Override
  public void writeByteArray(byte[] value) {
    delegate.writeByteArray(value);
  }

  @Override
  public void writeByte(byte value) {
    delegate.writeByte(value);
  }

  @Override
  public void writeInt(int value) {
    delegate.writeInt(value);
  }

  @Override
  public void writeLong(long value) {
    delegate.writeLong(value);
  }

  @Override
  public void writeUInt256(UInt256 value) {
    delegate.writeUInt256(value);
  }

  @Override
  public void writeBigInteger(BigInteger value) {
    delegate.writeBigInteger(value);
  }

  @Override
  public void writeString(String str) {
    delegate.writeString(str);
  }

  @Override
  public void writeList(Consumer<RLPWriter> fn) {
    delegate.writeList(fn);
  }
}
