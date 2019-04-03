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

import static java.util.Objects.requireNonNull;
import static org.apache.tuweni.rlp.RLP.encodeByteArray;
import static org.apache.tuweni.rlp.RLP.encodeLength;
import static org.apache.tuweni.rlp.RLP.encodeNumber;

import org.apache.tuweni.bytes.Bytes;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

final class AccumulatingRLPWriter implements RLPWriter {

  private static final int COMBINE_THRESHOLD = 32;

  private ArrayDeque<byte[]> values = new ArrayDeque<>();

  Deque<byte[]> values() {
    return values;
  }

  @Override
  public void writeRLP(Bytes value) {
    requireNonNull(value);
    appendBytes(value.toArrayUnsafe());
  }

  @Override
  public void writeValue(Bytes value) {
    requireNonNull(value);
    writeByteArray(value.toArrayUnsafe());
  }

  @Override
  public void writeByteArray(byte[] value) {
    encodeByteArray(value, this::appendBytes);
  }

  @Override
  public void writeByte(byte value) {
    encodeByteArray(new byte[] {value}, this::appendBytes);
  }

  @Override
  public void writeLong(long value) {
    appendBytes(encodeNumber(value));
  }

  @Override
  public void writeList(Consumer<RLPWriter> fn) {
    requireNonNull(fn);
    AccumulatingRLPWriter listWriter = new AccumulatingRLPWriter();
    fn.accept(listWriter);
    int totalSize = 0;
    for (byte[] value : listWriter.values) {
      try {
        totalSize = Math.addExact(totalSize, value.length);
      } catch (ArithmeticException e) {
        throw new IllegalArgumentException("Combined length of values is too long (> Integer.MAX_VALUE)");
      }
    }
    appendBytes(encodeLength(totalSize, 0xc0));
    this.values.addAll(listWriter.values);
  }

  private void appendBytes(byte[] bytes) {
    if (bytes.length < COMBINE_THRESHOLD) {
      if (!values.isEmpty()) {
        byte[] last = values.getLast();
        if (last.length <= (COMBINE_THRESHOLD - bytes.length)) {
          byte[] combined = new byte[last.length + bytes.length];
          System.arraycopy(last, 0, combined, 0, last.length);
          System.arraycopy(bytes, 0, combined, last.length, bytes.length);
          values.pollLast();
          values.add(combined);
          return;
        }
      }
    }
    values.add(bytes);
  }
}
