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

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

import java.math.BigInteger;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


/**
 * A writer for encoding values to RLP.
 */
public interface RLPWriter {

  /**
   * Append an already RLP encoded value.
   *
   * <p>
   * Note that this method <b>may not</b> validate that {@code value} is a valid RLP sequence. Appending an invalid RLP
   * sequence will cause the entire RLP encoding produced by this writer to also be invalid.
   *
   * @param value The RLP encoded bytes to append.
   */
  void writeRLP(Bytes value);

  /**
   * Encode a {@link Bytes} value to RLP.
   *
   * @param value The byte array to encode.
   */
  void writeValue(Bytes value);

  /**
   * Encode a byte array to RLP.
   *
   * @param value The byte array to encode.
   */
  default void writeByteArray(byte[] value) {
    writeValue(Bytes.wrap(value));
  }

  /**
   * Encode a byte to RLP.
   *
   * @param value The byte value to encode.
   */
  default void writeByte(byte value) {
    writeValue(Bytes.of(value));
  }

  /**
   * Write an integer to the output.
   *
   * @param value The integer to write.
   */
  default void writeInt(int value) {
    writeLong(value);
  }

  /**
   * Write a long to the output.
   *
   * @param value The long value to write.
   */
  void writeLong(long value);

  /**
   * Write a {@link UInt256} to the output.
   *
   * @param value The {@link UInt256} value to write.
   */
  default void writeUInt256(UInt256 value) {
    writeValue(value.toMinimalBytes());
  }

  /**
   * Write a big integer to the output.
   *
   * @param value The integer to write.
   */
  default void writeBigInteger(BigInteger value) {
    if (value.signum() == 0) {
      writeInt(0);
      return;
    }
    byte[] byteArray = value.toByteArray();
    if (byteArray[0] == 0) {
      writeValue(Bytes.wrap(byteArray).slice(1));
    } else {
      writeByteArray(byteArray);
    }
  }

  /**
   * Write a string to the output.
   *
   * @param str The string to write.
   */
  default void writeString(String str) {
    writeByteArray(str.getBytes(UTF_8));
  }

  /**
   * Write a list of values.
   *
   * @param fn A consumer that will be provided with a {@link RLPWriter} that can consume values.
   */
  void writeList(Consumer<RLPWriter> fn);

  /**
   * Write a list of values, sending each value to a function to be interpreted.
   *
   * @param elements the list of elements to write
   * @param elementWriter the function called for each element in the list
   * @param <T> The type of the list elements.
   */
  default <T> void writeList(List<T> elements, BiConsumer<RLPWriter, T> elementWriter) {
    writeList(writer -> {
      for (T element : elements) {
        elementWriter.accept(writer, element);
      }
    });
  }
}
