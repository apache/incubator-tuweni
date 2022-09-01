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

import java.math.BigInteger;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Recursive Length Prefix (RLP) encoding and decoding.
 */
public final class RLP {
  private static final byte[] EMPTY_VALUE = new byte[] {(byte) 0x80};

  private RLP() {}

  /**
   * Encode values to a {@link Bytes} value.
   * <p>
   * Important: this method does not write any list prefix to the result. If you are writing a RLP encoded list of
   * values, you usually want to use {@link #encodeList(Consumer)}.
   *
   * @param fn A consumer that will be provided with a {@link RLPWriter} that can consume values.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encode(Consumer<RLPWriter> fn) {
    requireNonNull(fn);
    BytesRLPWriter writer = new BytesRLPWriter();
    fn.accept(writer);
    return writer.toBytes();
  }

  /**
   * Encode values to a {@link ByteBuffer}.
   * <p>
   * Important: this method does not write any list prefix to the result. If you are writing a RLP encoded list of
   * values, you usually want to use {@link #encodeList(Consumer)}.
   *
   * @param buffer The buffer to write into, starting from its current position.
   * @param fn A consumer that will be provided with a {@link RLPWriter} that can consume values.
   * @param <T> The type of the buffer.
   * @return The buffer.
   * @throws BufferOverflowException If the writer attempts to write more than the provided buffer can hold.
   * @throws ReadOnlyBufferException If the provided buffer is read-only.
   */
  public static <T extends ByteBuffer> T encodeTo(T buffer, Consumer<RLPWriter> fn) {
    requireNonNull(fn);
    ByteBufferRLPWriter writer = new ByteBufferRLPWriter(buffer);
    fn.accept(writer);
    return buffer;
  }

  /**
   * Encode a list of values to a {@link Bytes} value.
   *
   * @param fn A consumer that will be provided with a {@link RLPWriter} that can consume values.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeList(Consumer<RLPWriter> fn) {
    requireNonNull(fn);
    BytesRLPWriter writer = new BytesRLPWriter();
    writer.writeList(fn);
    return writer.toBytes();
  }

  /**
   * Encode a list of values to a {@link Bytes} value.
   *
   * @param elements A list of values to be encoded.
   * @param fn A consumer that will be provided with a {@link RLPWriter} and an element of the list.
   * @param <T> The type of the list elements.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static <T> Bytes encodeList(List<T> elements, BiConsumer<RLPWriter, T> fn) {
    requireNonNull(fn);
    BytesRLPWriter writer = new BytesRLPWriter();
    writer.writeList(elements, fn);
    return writer.toBytes();
  }

  /**
   * Encode a list of values to a {@link ByteBuffer}.
   *
   * @param buffer The buffer to write into, starting from its current position.
   * @param fn A consumer that will be provided with a {@link RLPWriter} that can consume values.
   * @param <T> The type of the buffer.
   * @return The buffer.
   * @throws BufferOverflowException If the writer attempts to write more than the provided buffer can hold.
   * @throws ReadOnlyBufferException If the provided buffer is read-only.
   */
  public static <T extends ByteBuffer> T encodeListTo(T buffer, Consumer<RLPWriter> fn) {
    requireNonNull(fn);
    ByteBufferRLPWriter writer = new ByteBufferRLPWriter(buffer);
    writer.writeList(fn);
    return buffer;
  }

  /**
   * Encode a value to a {@link Bytes} value.
   *
   * @param value The value to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeValue(Bytes value) {
    requireNonNull(value);
    return encodeValue(value.toArrayUnsafe());
  }

  /**
   * Encode a value to a {@link Bytes} value.
   *
   * @param value The value to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeByteArray(byte[] value) {
    requireNonNull(value);
    return encodeValue(value);
  }

  private static Bytes encodeValue(byte[] value) {
    int maxSize = value.length + 5;
    ByteBuffer buffer = ByteBuffer.allocate(maxSize);
    encodeByteArray(value, buffer::put);
    return Bytes.wrap(buffer.array(), 0, buffer.position());
  }

  static void encodeByteArray(byte[] value, Consumer<byte[]> appender) {
    requireNonNull(value);
    int size = value.length;
    if (size == 0) {
      appender.accept(EMPTY_VALUE);
      return;
    }
    if (size == 1) {
      byte b = value[0];
      if ((b & 0xFF) <= 0x7f) {
        appender.accept(value);
        return;
      }
    }
    appender.accept(encodeLength(size, 0x80));
    appender.accept(value);
  }

  /**
   * Encode a integer to a {@link Bytes} value.
   *
   * @param value The integer to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeInt(int value) {
    return encodeLong(value);
  }

  /**
   * Encode a long to a {@link Bytes} value.
   *
   * @param value The long to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeLong(long value) {
    return Bytes.wrap(encodeNumber(value));
  }

  static byte[] encodeNumber(long value) {
    if (value == 0x00) {
      return EMPTY_VALUE;
    }
    if (value <= 0x7f) {
      return new byte[] {(byte) (value & 0xFF)};
    }
    return encodeLongBytes(value, 0x80);
  }

  private static byte[] encodeLongBytes(long value, int offset) {
    int zeros = Long.numberOfLeadingZeros(value);
    int resultBytes = 8 - (zeros / 8);

    byte[] encoded = new byte[resultBytes + 1];
    encoded[0] = (byte) ((offset + resultBytes) & 0xFF);

    int shift = 0;
    for (int i = 0; i < resultBytes; i++) {
      encoded[resultBytes - i] = (byte) ((value >> shift) & 0xFF);
      shift += 8;
    }
    return encoded;
  }

  /**
   * Encode a big integer to a {@link Bytes} value.
   *
   * @param value The big integer to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeBigInteger(BigInteger value) {
    requireNonNull(value);
    return encode(writer -> writer.writeBigInteger(value));
  }

  /**
   * Encode a string to a {@link Bytes} value.
   *
   * @param str The string to encode.
   * @return The RLP encoding in a {@link Bytes} value.
   */
  public static Bytes encodeString(String str) {
    requireNonNull(str);
    return encodeByteArray(str.getBytes(UTF_8));
  }

  static byte[] encodeLength(int length, int offset) {
    if (length <= 55) {
      return new byte[] {(byte) ((offset + length) & 0xFF)};
    }
    return encodeLongBytes(length, offset + 55);
  }

  /**
   * Read and decode RLP from a {@link Bytes} value.
   * <p>
   * Important: this method does not consume any list prefix from the source data. If you are reading a RLP encoded list
   * of values, you usually want to use {@link #decodeList(Bytes, Function)}.
   *
   * @param source The RLP encoded bytes.
   * @param fn A function that will be provided a {@link RLPReader}.
   * @param <T> The result type of the reading function.
   * @return The result from the reading function.
   */
  public static <T> T decode(Bytes source, Function<RLPReader, T> fn) {
    return decode(source, false, fn);
  }

  /**
   * Read and decode RLP from a {@link Bytes} value.
   * <p>
   * Important: this method does not consume any list prefix from the source data. If you are reading a RLP encoded list
   * of values, you usually want to use {@link #decodeList(Bytes, Function)}.
   *
   * @param source The RLP encoded bytes.
   * @param lenient If {@code false}, an exception will be thrown if the value is not minimally encoded.
   * @param fn A function that will be provided a {@link RLPReader}.
   * @param <T> The result type of the reading function.
   * @return The result from the reading function.
   */
  public static <T> T decode(Bytes source, boolean lenient, Function<RLPReader, T> fn) {
    requireNonNull(source);
    requireNonNull(fn);
    return fn.apply(new BytesRLPReader(source, lenient));
  }

  /**
   * Read an RLP encoded list of values from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @param fn A function that will be provided a {@link RLPReader}.
   * @param <T> The result type of the reading function.
   * @return The result from the reading function.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the first RLP value is not a list.
   */
  public static <T> T decodeList(Bytes source, Function<RLPReader, T> fn) {
    return decodeList(source, false, fn);
  }

  /**
   * Read an RLP encoded list of values from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @param lenient If {@code false}, an exception will be thrown if the value is not minimally encoded.
   * @param fn A function that will be provided a {@link RLPReader}.
   * @param <T> The result type of the reading function.
   * @return The result from the reading function.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the first RLP value is not a list.
   */
  public static <T> T decodeList(Bytes source, boolean lenient, Function<RLPReader, T> fn) {
    requireNonNull(source);
    requireNonNull(fn);
    if (source.isEmpty()) {
      throw new IllegalArgumentException("source is empty");
    }
    return decode(source, lenient, reader -> reader.readList(fn));
  }

  /**
   * Read an RLP encoded list of values from a {@link Bytes} value, populating a mutable output list.
   *
   * @param source The RLP encoded bytes.
   * @param fn A function that will be provided a {@link RLPReader}.
   * @return The list supplied to {@code fn}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the first RLP value is not a list.
   */
  public static List<Object> decodeToList(Bytes source, BiConsumer<RLPReader, List<Object>> fn) {
    return decodeToList(source, false, fn);
  }

  /**
   * Read an RLP encoded list of values from a {@link Bytes} value, populating a mutable output list.
   *
   * @param source The RLP encoded bytes.
   * @param lenient If {@code false}, an exception will be thrown if the value is not minimally encoded.
   * @param fn A function that will be provided a {@link RLPReader}.
   * @return The list supplied to {@code fn}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the first RLP value is not a list.
   */
  public static List<Object> decodeToList(Bytes source, boolean lenient, BiConsumer<RLPReader, List<Object>> fn) {
    requireNonNull(source);
    requireNonNull(fn);
    if (source.isEmpty()) {
      throw new IllegalArgumentException("source is empty");
    }
    return decode(source, lenient, reader -> reader.readList(fn));
  }

  /**
   * Read a list of values from the RLP source, populating a list using a function interpreting each value.
   *
   * @param source The RLP encoded bytes.
   * @param fn A function creating a new element of the list for each value in the RLP list.
   * @return The list supplied to {@code fn}.
   * @param <T> The type of the list elements.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the first RLP value is not a list.
   */
  public static <T> List<T> decodeToList(Bytes source, Function<RLPReader, T> fn) {
    return decodeToList(source, false, fn);
  }

  /**
   * Read an RLP encoded list of values from a {@link Bytes} value, populating a mutable output list.
   *
   * @param source The RLP encoded bytes.
   * @param lenient If {@code false}, an exception will be thrown if the value is not minimally encoded.
   * @param fn A function creating a new element of the list for each value in the RLP list.
   * @param <T> The type of the list elements.
   * @return The list supplied to {@code fn}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws InvalidRLPTypeException If the first RLP value is not a list.
   */
  public static <T> List<T> decodeToList(Bytes source, boolean lenient, Function<RLPReader, T> fn) {
    requireNonNull(source);
    requireNonNull(fn);
    if (source.isEmpty()) {
      throw new IllegalArgumentException("source is empty");
    }
    return decode(source, lenient, reader -> reader.readListContents(fn));
  }

  /**
   * Read an RLP encoded value from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @return The bytes for the value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no RLP values to read.
   */
  public static Bytes decodeValue(Bytes source) {
    return decodeValue(source, false);
  }

  /**
   * Read an RLP encoded value from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @param lenient If {@code false}, an exception will be thrown if the value is not minimally encoded.
   * @return The bytes for the value.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   * @throws EndOfRLPException If there are no RLP values to read.
   */
  public static Bytes decodeValue(Bytes source, boolean lenient) {
    requireNonNull(source);
    return decode(source, lenient, RLPReader::readValue);
  }

  /**
   * Read an RLP encoded integer from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @return An integer.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static int decodeInt(Bytes source) {
    return decodeInt(source, false);
  }

  /**
   * Read an RLP encoded integer from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @param lenient If {@code false}, an exception will be thrown if the value is not minimally encoded.
   * @return An integer.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static int decodeInt(Bytes source, boolean lenient) {
    requireNonNull(source);
    if (source.isEmpty()) {
      throw new IllegalArgumentException("source is empty");
    }
    return decode(source, lenient, RLPReader::readInt);
  }

  /**
   * Read an RLP encoded long from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @return A long.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static long decodeLong(Bytes source) {
    return decodeLong(source, false);
  }

  /**
   * Read an RLP encoded long from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @param lenient If {@code false}, an exception will be thrown if the value is not minimally encoded.
   * @return A long.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static long decodeLong(Bytes source, boolean lenient) {
    requireNonNull(source);
    if (source.isEmpty()) {
      throw new IllegalArgumentException("source is empty");
    }
    return decode(source, lenient, RLPReader::readLong);
  }

  /**
   * Read an RLP encoded big integer from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @return A {@link BigInteger}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static BigInteger decodeBigInteger(Bytes source) {
    return decodeBigInteger(source, false);
  }

  /**
   * Read an RLP encoded big integer from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @param lenient If {@code false}, an exception will be thrown if the value is not minimally encoded.
   * @return A {@link BigInteger}.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static BigInteger decodeBigInteger(Bytes source, boolean lenient) {
    requireNonNull(source);
    if (source.isEmpty()) {
      throw new IllegalArgumentException("source is empty");
    }
    return decode(source, lenient, RLPReader::readBigInteger);
  }

  /**
   * Read an RLP encoded string from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @return A string.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static String decodeString(Bytes source) {
    return decodeString(source, false);
  }

  /**
   * Read an RLP encoded string from a {@link Bytes} value.
   *
   * @param source The RLP encoded bytes.
   * @param lenient If {@code false}, an exception will be thrown if the value is not minimally encoded.
   * @return A string.
   * @throws InvalidRLPEncodingException If there is an error decoding the RLP source.
   */
  public static String decodeString(Bytes source, boolean lenient) {
    requireNonNull(source);
    if (source.isEmpty()) {
      throw new IllegalArgumentException("source is empty");
    }
    return decode(source, lenient, RLPReader::readString);
  }

  /**
   * Check if the {@link Bytes} value contains an RLP encoded list.
   *
   * @param value The value to check.
   * @return {@code true} if the value contains a list.
   */
  public static boolean isList(Bytes value) {
    requireNonNull(value);
    if (value.isEmpty()) {
      throw new IllegalArgumentException("value is empty");
    }
    return decode(value, RLPReader::nextIsList);
  }
}
