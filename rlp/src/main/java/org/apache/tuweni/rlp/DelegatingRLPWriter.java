/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
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
