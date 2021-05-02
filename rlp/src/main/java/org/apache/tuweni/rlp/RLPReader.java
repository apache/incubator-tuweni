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
import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A reader for consuming values from an RLP encoded source.
 */
public interface RLPReader {

  /**
   * Determine if this reader is lenient by default.
   * <p>
   * A non-lenient reader will throw {@link InvalidRLPEncodingException} from any read method if the source RLP has not
   * used a minimal encoding format for the value.
   * 
   * @return {@code true} if the reader is lenient, and {@code false} otherwise (default).
   */
  boolean isLenient();

  /**
   * Read the next value from the RLP source.
   *
   * @return The bytes for the next value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default Bytes readValue() {
    return readValue(isLenient());
  }

  /**
   * Read the next value from the RLP source.
   *
   * @param lenient If {@code false}, an exception will be thrown if the value is not minimally encoded.
   * @return The bytes for the next value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  Bytes readValue(boolean lenient);

  /**
   * Read a byte array from the RLP source.
   *
   * @return The byte array for the next value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default byte[] readByteArray() {
    return readValue().toArrayUnsafe();
  }

  /**
   * Read a byte from the RLP source.
   *
   * @return The byte for the next value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default byte readByte() {
    return readByte(isLenient());
  }

  /**
   * Read a byte from the RLP source.
   *
   * @param lenient If {@code false}, an exception will be thrown if the byte is not minimally encoded.
   * @return The byte for the next value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default byte readByte(boolean lenient) {
    Bytes bytes = readValue(lenient);
    if (bytes.size() != 1) {
      throw new InvalidRLPTypeException("Value is not a single byte");
    }
    return bytes.get(0);
  }

  /**
   * Read an integer value from the RLP source.
   *
   * @return An integer.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as an integer.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default int readInt() {
    return readInt(isLenient());
  }

  /**
   * Read an integer value from the RLP source.
   *
   * @param lenient If {@code false}, an exception will be thrown if the integer is not minimally encoded.
   * @return An integer.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source, or the integer is not minimally
   *         encoded and `lenient` is {@code false}.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as an integer.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default int readInt(boolean lenient) {
    Bytes bytes = readValue();
    if (!lenient && bytes.hasLeadingZeroByte()) {
      throw new InvalidRLPEncodingException("Integer value was not minimally encoded");
    }
    try {
      return bytes.toInt();
    } catch (IllegalArgumentException e) {
      throw new InvalidRLPTypeException("Value is too large to be represented as an int");
    }
  }

  /**
   * Read a long value from the RLP source.
   *
   * @return A long.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a long.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default long readLong() {
    return readLong(isLenient());
  }

  /**
   * Read a long value from the RLP source.
   *
   * @param lenient If {@code false}, an exception will be thrown if the integer is not minimally encoded.
   * @return A long.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source, or the integer is not minimally
   *         encoded and `lenient` is {@code false}.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a long.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default long readLong(boolean lenient) {
    Bytes bytes = readValue();
    if (!lenient && bytes.hasLeadingZeroByte()) {
      throw new InvalidRLPEncodingException("Integer value was not minimally encoded");
    }
    try {
      return bytes.toLong();
    } catch (IllegalArgumentException e) {
      throw new InvalidRLPTypeException("Value is too large to be represented as a long");
    }
  }

  /**
   * Read a {@link UInt256} value from the RLP source.
   *
   * @return A {@link UInt256} value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a long.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default UInt256 readUInt256() {
    return readUInt256(isLenient());
  }

  /**
   * Read a {@link UInt256} value from the RLP source.
   *
   * @param lenient If {@code false}, an exception will be thrown if the integer is not minimally encoded.
   * @return A {@link UInt256} value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source, or the integer is not minimally
   *         encoded and `lenient` is {@code false}.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a long.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default UInt256 readUInt256(boolean lenient) {
    Bytes bytes = readValue();
    if (!lenient && bytes.hasLeadingZeroByte()) {
      throw new InvalidRLPEncodingException("Integer value was not minimally encoded");
    }
    try {
      return UInt256.fromBytes(bytes);
    } catch (IllegalArgumentException e) {
      throw new InvalidRLPTypeException("Value is too large to be represented as a UInt256");
    }
  }

  /**
   * Read a big integer value from the RLP source.
   *
   * @return A big integer.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a big integer.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default BigInteger readBigInteger() {
    return readBigInteger(isLenient());
  }

  /**
   * Read a big integer value from the RLP source.
   *
   * @param lenient If {@code false}, an exception will be thrown if the integer is not minimally encoded.
   * @return A big integer.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source, or the integer is not minimally
   *         encoded and `lenient` is {@code false}.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a big integer.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default BigInteger readBigInteger(boolean lenient) {
    Bytes bytes = readValue();
    if (!lenient && bytes.hasLeadingZeroByte()) {
      throw new InvalidRLPEncodingException("Integer value was not minimally encoded");
    }
    return bytes.toUnsignedBigInteger();
  }

  /**
   * Read a string value from the RLP source.
   *
   * @return A string.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a string.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default String readString() {
    return readString(isLenient());
  }

  /**
   * Read a string value from the RLP source.
   *
   * @param lenient If {@code false}, an exception will be thrown if the integer is not minimally encoded.
   * @return A string.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value cannot be represented as a string.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default String readString(boolean lenient) {
    return new String(readValue(lenient).toArrayUnsafe(), UTF_8);
  }

  /**
   * Check if the next item to be read is a list.
   *
   * @return {@code true} if the next item to be read is a list.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  boolean nextIsList();

  /**
   * Check if the next item to be read is empty.
   *
   * @return {@code true} if the next item to be read is empty.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  boolean nextIsEmpty();

  /**
   * Read a list of values from the RLP source.
   *
   * @param fn A function that will be provided a {@link RLPReader}.
   * @param <T> The result type of the reading function.
   * @return The result from the reading function.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value is not a list.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default <T> T readList(Function<RLPReader, T> fn) {
    return readList(isLenient(), fn);
  }

  /**
   * Read a list of values from the RLP source.
   *
   * @param lenient If {@code false}, an exception will be thrown if the integer is not minimally encoded.
   * @param fn A function that will be provided a {@link RLPReader}.
   * @param <T> The result type of the reading function.
   * @return The result from the reading function.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value is not a list.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  <T> T readList(boolean lenient, Function<RLPReader, T> fn);

  /**
   * Read a list of values from the RLP source, populating a mutable output list.
   *
   * @param fn A function that will be provided with a {@link RLPReader} and a mutable output list.
   * @return The list supplied to {@code fn}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value is not a list.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default List<Object> readList(BiConsumer<RLPReader, List<Object>> fn) {
    return readList(isLenient(), fn);
  }

  /**
   * Read a list of values from the RLP source, populating a list using a function interpreting each value.
   *
   * @param fn A function creating a new element of the list for each value in the RLP list.
   * @param <T> The type of the list elements.
   * @return The list supplied to {@code fn}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value is not a list.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default <T> List<T> readListContents(Function<RLPReader, T> fn) {
    return readListContents(isLenient(), fn);
  }

  /**
   * Read a list of values from the RLP source, populating a mutable output list.
   *
   * @param lenient If {@code false}, an exception will be thrown if the integer is not minimally encoded.
   * @param fn A function that will be provided with a {@link RLPReader} and a mutable output list.
   * @return The list supplied to {@code fn}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value is not a list.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default List<Object> readList(boolean lenient, BiConsumer<RLPReader, List<Object>> fn) {
    requireNonNull(fn);
    return readList(lenient, reader -> {
      List<Object> list = new ArrayList<>();
      fn.accept(reader, list);
      return list;
    });
  }

  /**
   * Read a list of values from the RLP source, populating a list using a function interpreting each value.
   *
   * @param lenient If {@code false}, an exception will be thrown if the integer is not minimally encoded.
   * @param fn A function creating a new element of the list for each value in the RLP list.
   * @param <T> The type of the list elements.
   * @return The list supplied to {@code fn}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the next RLP value is not a list.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default <T> List<T> readListContents(boolean lenient, Function<RLPReader, T> fn) {
    requireNonNull(fn);

    return readList(lenient, reader -> {
      List<T> list = new ArrayList<T>();
      while (!reader.isComplete()) {
        list.add(fn.apply(reader));
      }
      return list;
    });
  }

  /**
   * Skip the next value or list in the RLP source.
   *
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  default void skipNext() {
    skipNext(isLenient());
  }

  /**
   * Skip the next value or list in the RLP source.
   *
   * @param lenient If {@code false}, an exception will be thrown if the integer is not minimally encoded.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no more RLP values to read.
   */
  void skipNext(boolean lenient);

  /**
   * The number of remaining values to read.
   *
   * @return The number of remaining values to read.
   */
  int remaining();

  /**
   * Check if all values have been read.
   *
   * @return {@code true} if all values have been read.
   */
  boolean isComplete();

  /**
   * Returns reader's index
   *
   * @return current reader position
   */
  int position();

  /**
   * Provides the remainder of the bytes that have not been read yet.
   * 
   * @return the remainder of the input at current position
   */
  Bytes readRemaining();
}
