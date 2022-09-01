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
package org.apache.tuweni.ssz;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.Hash;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.bigints.UInt384;

import java.math.BigInteger;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Simple Serialize (SSZ) encoding and decoding.
 */
public final class SSZ {

  private static final Bytes TRUE = Bytes.of((byte) 1);
  private static final Bytes FALSE = Bytes.of((byte) 0);

  private SSZ() {}

  /**
   * Create the hash tree root of a set of values
   *
   * @param bytes 1 value or a list of homogeneous values
   * @return the SSZ tree root hash of the values
   */
  public static Bytes32 hashTreeRoot(Bytes... bytes) {
    if (bytes.length == 1) {
      if (bytes[0].size() > 32) {
        return Hash.keccak256(bytes[0]);
      } else {
        return Bytes32.rightPad(bytes[0]);
      }
    } else {
      Bytes hash = merkleHash(new ArrayList<>(Arrays.asList(bytes)));
      return Bytes32.rightPad(hash);
    }
  }

  /**
   * Hashes a list of homogeneous values.
   *
   * @param values a list of homogeneous values
   *
   * @return the merkle hash of the list of values
   */
  static Bytes merkleHash(List<Bytes> values) {
    Bytes littleEndianLength = Bytes.ofUnsignedInt(values.size(), LITTLE_ENDIAN);
    Bytes32 valuesLength = Bytes32.rightPad(littleEndianLength);

    List<Bytes> chunks;
    if (values.isEmpty()) {
      chunks = new ArrayList<>();
      chunks.add(Bytes.wrap(new byte[128]));
    } else if (values.get(0).size() < 128) {
      int itemsPerChunk = (int) Math.floor(128 / (double) values.get(0).size());
      chunks = new ArrayList<>();

      for (int i = 0; i * itemsPerChunk < values.size(); i++) {
        Bytes[] chunkItems =
            values.subList(i * itemsPerChunk, Math.min((i + 1) * itemsPerChunk, values.size())).toArray(new Bytes[0]);
        chunks.add(Bytes.concatenate(chunkItems));
      }
    } else {
      chunks = values;
    }
    while (chunks.size() > 1) {
      if (chunks.size() % 2 == 1) {
        chunks.add(Bytes.wrap(new byte[128]));
      }
      Iterator<Bytes> iterator = chunks.iterator();
      List<Bytes> hashRound = new ArrayList<>();
      while (iterator.hasNext()) {
        hashRound.add(Hash.keccak256(Bytes.concatenate(iterator.next(), iterator.next())));
      }
      chunks = hashRound;
    }

    return Hash.keccak256(Bytes.concatenate(chunks.get(0), valuesLength));
  }

  // Encoding

  /**
   * Encode values to a {@link Bytes} value.
   *
   * @param fn A consumer that will be provided with a {@link SSZWriter} that can consume values.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encode(Consumer<SSZWriter> fn) {
    requireNonNull(fn);
    BytesSSZWriter writer = new BytesSSZWriter();
    fn.accept(writer);
    return writer.toBytes();
  }

  /**
   * Encode values to a {@link ByteBuffer}.
   *
   * @param buffer The buffer to write into, starting from its current position.
   * @param fn A consumer that will be provided with a {@link SSZWriter} that can consume values.
   * @param <T> The type of the buffer.
   * @return The buffer.
   * @throws BufferOverflowException if the writer attempts to write more than the provided buffer can hold
   * @throws ReadOnlyBufferException if the provided buffer is read-only
   */
  public static <T extends ByteBuffer> T encodeTo(T buffer, Consumer<SSZWriter> fn) {
    requireNonNull(buffer);
    requireNonNull(fn);
    ByteBufferSSZWriter writer = new ByteBufferSSZWriter(buffer);
    fn.accept(writer);
    return buffer;
  }

  /**
   * Encode {@link Bytes}.
   *
   * @param value The value to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeBytes(Bytes value) {
    Bytes lengthBytes = encodeLong(value.size(), 32);
    return Bytes.wrap(lengthBytes, value);
  }

  static void encodeBytesTo(Bytes value, Consumer<Bytes> appender) {
    appender.accept(encodeLong(value.size(), 32));
    appender.accept(value);
  }

  static void encodeFixedBytesTo(Bytes value, Consumer<Bytes> appender) {
    appender.accept(value);
  }

  /**
   * Encode a value to a {@link Bytes} value.
   *
   * @param value The value to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeByteArray(byte[] value) {
    return encodeBytes(Bytes.wrap(value));
  }

  static void encodeByteArrayTo(byte[] value, Consumer<byte[]> appender) {
    appender.accept(encodeLongToByteArray(value.length, 32));
    appender.accept(value);
  }

  /**
   * Encode a string to a {@link Bytes} value.
   *
   * @param str The string to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  public static Bytes encodeString(String str) {
    return encodeByteArray(str.getBytes(UTF_8));
  }

  static void encodeStringTo(String str, Consumer<byte[]> appender) {
    encodeByteArrayTo(str.getBytes(UTF_8), appender);
  }

  /**
   * Encode a two's-compliment integer to a {@link Bytes} value.
   *
   * @param value the integer to encode
   * @param bitLength the bit length of the encoded integer value (must be a multiple of 8)
   * @return the SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if the value is too large for the specified {@code bitLength}
   */
  public static Bytes encodeInt(int value, int bitLength) {
    return encodeLong(value, bitLength);
  }

  /**
   * Encode a two's-compliment long integer to a {@link Bytes} value.
   *
   * @param value the long to encode
   * @param bitLength the bit length of the integer value (must be a multiple of 8)
   * @return the SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if the value is too large for the specified {@code bitLength}
   */
  public static Bytes encodeLong(long value, int bitLength) {
    return Bytes.wrap(encodeLongToByteArray(value, bitLength));
  }

  static byte[] encodeLongToByteArray(long value, int bitLength) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    int zeros = (value >= 0) ? Long.numberOfLeadingZeros(value) : Long.numberOfLeadingZeros(-1 - value) - 1;
    int valueBytes = 8 - (zeros / 8);

    int byteLength = bitLength / 8;
    if (valueBytes > byteLength) {
      throw new IllegalArgumentException("value is too large for the desired bitLength");
    }

    byte[] encoded = new byte[byteLength];

    int shift = 0;
    for (int i = 0; i < valueBytes; i++) {
      encoded[i] = (byte) ((value >> shift) & 0xFF);
      shift += 8;
    }
    if (value < 0) {
      // Extend the two's-compliment integer by setting all remaining bits to 1.
      for (int i = valueBytes; i < byteLength; i++) {
        encoded[i] = (byte) 0xFF;
      }
    }
    return encoded;
  }

  /**
   * Encode a big integer to a {@link Bytes} value.
   *
   * @param value the big integer to encode
   * @param bitLength the bit length of the integer value (must be a multiple of 8)
   * @return the SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if the value is too large for the specified {@code bitLength}
   */
  public static Bytes encodeBigInteger(BigInteger value, int bitLength) {
    return Bytes.wrap(encodeBigIntegerToByteArray(value, bitLength));
  }

  public static byte[] encodeBigIntegerToByteArray(BigInteger value, int bitLength) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    byte[] bytes = value.toByteArray();
    int valueBytes = bytes.length;
    int offset = 0;
    if (value.signum() >= 0 && bytes[0] == 0) {
      valueBytes = bytes.length - 1;
      offset = 1;
    }

    int byteLength = bitLength / 8;
    if (valueBytes > byteLength) {
      throw new IllegalArgumentException("value is too large for the desired bitLength");
    }

    byte[] encoded;
    if (valueBytes == byteLength && offset == 0) {
      encoded = bytes;
    } else {
      encoded = new byte[byteLength];
      int padLength = byteLength - valueBytes;
      System.arraycopy(bytes, offset, encoded, padLength, valueBytes);
      if (value.signum() < 0) {
        // Extend the two's-compliment integer by setting all leading bits to 1.
        for (int i = 0; i < padLength; i++) {
          encoded[i] = (byte) 0xFF;
        }
      }
    }
    // reverse the array to make it little endian
    for (int i = 0; i < (encoded.length / 2); i++) {
      byte swapped = encoded[i];
      encoded[i] = encoded[encoded.length - i - 1];
      encoded[encoded.length - i - 1] = swapped;
    }
    return encoded;
  }

  /**
   * Encode an 8-bit two's-compliment integer to a {@link Bytes} value.
   *
   * @param value the integer to encode
   * @return the SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if the value is too large to be represented in 8 bits
   */
  public static Bytes encodeInt8(int value) {
    return encodeInt(value, 8);
  }

  /**
   * Encode a 16-bit two's-compliment integer to a {@link Bytes} value.
   *
   * @param value the integer to encode
   * @return the SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if the value is too large to be represented in 16 bits
   */
  public static Bytes encodeInt16(int value) {
    return encodeInt(value, 16);
  }

  /**
   * Encode a 32-bit two's-compliment integer to a {@link Bytes} value.
   *
   * @param value the integer to encode
   * @return the SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeInt32(int value) {
    return encodeInt(value, 32);
  }

  /**
   * Encode a 64-bit two's-compliment integer to a {@link Bytes} value.
   *
   * @param value the integer to encode
   * @return the SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeInt64(long value) {
    return encodeLong(value, 64);
  }

  /**
   * Encode an unsigned integer to a {@link Bytes} value.
   *
   * Note that {@code value} is a native signed int, but will be interpreted as an unsigned value.
   *
   * @param value the integer to encode
   * @param bitLength the bit length of the encoded integer value (must be a multiple of 8)
   * @return the SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if the value is too large for the specified {@code bitLength}
   */
  public static Bytes encodeUInt(int value, int bitLength) {
    return encodeULong(value, bitLength);
  }

  /**
   * Encode an unsigned long integer to a {@link Bytes} value.
   *
   * Note that {@code value} is a native signed long, but will be interpreted as an unsigned value.
   *
   * @param value the long to encode
   * @param bitLength the bit length of the integer value (must be a multiple of 8)
   * @return the SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if the value is too large for the specified {@code bitLength}
   */
  public static Bytes encodeULong(long value, int bitLength) {
    return Bytes.wrap(encodeULongToByteArray(value, bitLength));
  }

  static byte[] encodeULongToByteArray(long value, int bitLength) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    int zeros = Long.numberOfLeadingZeros(value);
    int valueBytes = 8 - (zeros / 8);
    if (zeros != 0 && value < 0) {
      throw new IllegalArgumentException("Value must be positive or zero");
    }

    int byteLength = bitLength / 8;
    if (valueBytes > byteLength) {
      throw new IllegalArgumentException("value is too large for the desired bitLength");
    }

    byte[] encoded = new byte[byteLength];

    int shift = 0;
    for (int i = 0; i < valueBytes; i++) {
      encoded[i] = (byte) ((value >> shift) & 0xFF);
      shift += 8;
    }
    return encoded;
  }

  /**
   * Encode an unsigned big integer to a {@link Bytes} value.
   *
   * @param value the big integer to encode
   * @param bitLength the bit length of the integer value (must be a multiple of 8)
   * @return the SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if the value is too large for the specified {@code bitLength}
   */
  public static Bytes encodeUBigInteger(BigInteger value, int bitLength) {
    return Bytes.wrap(encodeUBigIntegerToByteArray(value, bitLength));
  }

  public static byte[] encodeUBigIntegerToByteArray(BigInteger value, int bitLength) {
    if (value.compareTo(BigInteger.ZERO) < 0) {
      throw new IllegalArgumentException("Value must be positive or zero");
    }
    return encodeBigIntegerToByteArray(value, bitLength);
  }

  /**
   * Encode an 8-bit unsigned integer to a {@link Bytes} value.
   *
   * @param value the integer to encode
   * @return the SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if the value is too large to be represented in 8 bits
   */
  public static Bytes encodeUInt8(int value) {
    return encodeUInt(value, 8);
  }

  /**
   * Encode a 16-bit unsigned integer to a {@link Bytes} value.
   *
   * @param value the integer to encode
   * @return the SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if the value is too large to be represented in 16 bits
   */
  public static Bytes encodeUInt16(int value) {
    return encodeUInt(value, 16);
  }

  /**
   * Encode a 32-bit unsigned integer to a {@link Bytes} value.
   *
   * @param value the integer to encode
   * @return the SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeUInt32(long value) {
    return encodeULong(value, 32);
  }

  /**
   * Encode a 64-bit unsigned integer to a {@link Bytes} value.
   *
   * Note that {@code value} is a native signed long, but will be interpreted as an unsigned value.
   *
   * @param value the integer to encode
   * @return the SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeUInt64(long value) {
    return encodeULong(value, 64);
  }

  /**
   * Encode a 256-bit unsigned integer to a little-endian {@link Bytes} value.
   *
   * @param value the integer to encode
   * @return the SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeUInt256(UInt256 value) {
    return value.reverse();
  }

  /**
   * Encode a 384-bit unsigned integer to a little-endian {@link Bytes} value.
   *
   * @param value the integer to encode
   * @return the SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeUInt384(UInt384 value) {
    return value.toBytes().reverse();
  }

  /**
   * Encode a boolean to a {@link Bytes} value.
   *
   * @param value the boolean to encode
   * @return the SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeBoolean(boolean value) {
    return value ? TRUE : FALSE;
  }

  /**
   * Encode a 20-byte address to a {@link Bytes} value.
   *
   * @param address the address (must be exactly 20 bytes)
   * @return the SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if {@code address.size != 20}
   */
  public static Bytes encodeAddress(Bytes address) {
    if (address.size() != 20) {
      throw new IllegalArgumentException("address is not 20 bytes");
    }
    return address;
  }

  /**
   * Encode a hash to a {@link Bytes} value.
   *
   * @param hash the hash
   * @return the SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeHash(Bytes hash) {
    return hash;
  }

  /**
   * Encode a list of bytes.
   *
   * @param elements the bytes to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeBytesList(Bytes... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length * 2 + 1);
    encodeBytesListTo(elements, encoded::add);
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of bytes.
   *
   * @param elements the bytes to write as a list
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeBytesList(List<? extends Bytes> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() * 2 + 1);
    encodeBytesListTo(elements, encoded::add);
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeBytesListTo(Bytes[] elements, Consumer<Bytes> appender) {
    // pre-calculate the list size - relies on knowing how encodeBytesTo does its serialization, but is worth it
    // to avoid having to pre-serialize all the elements
    long listSize = 0;
    for (Bytes bytes : elements) {
      listSize += 4;
      listSize += bytes.size();
      if (listSize > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Cannot serialize list: overall length is too large");
      }
    }
    appender.accept(encodeUInt32(listSize));
    for (Bytes bytes : elements) {
      encodeBytesTo(bytes, appender);
    }
  }

  static void encodeBytesListTo(List<? extends Bytes> elements, Consumer<Bytes> appender) {
    // pre-calculate the list size - relies on knowing how encodeBytesTo does its serialization, but is worth it
    // to avoid having to pre-serialize all the elements
    long listSize = 0;
    for (Bytes bytes : elements) {
      listSize += 4;
      listSize += bytes.size();
      if (listSize > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Cannot serialize list: overall length is too large");
      }
    }
    appender.accept(encodeUInt32(listSize));
    for (Bytes bytes : elements) {
      encodeBytesTo(bytes, appender);
    }
  }

  static void encodeFixedBytesVectorTo(List<? extends Bytes> elements, Consumer<Bytes> appender) {
    for (Bytes bytes : elements) {
      appender.accept(bytes);
    }
  }

  static void encodeBytesVectorTo(List<? extends Bytes> elements, Consumer<Bytes> appender) {
    for (Bytes bytes : elements) {
      appender.accept(encodeLong(bytes.size(), 32));
      appender.accept(bytes);
    }
  }

  static void encodeFixedBytesListTo(List<? extends Bytes> elements, Consumer<Bytes> appender) {
    // pre-calculate the list size - relies on knowing how encodeBytesTo does its serialization, but is worth it
    // to avoid having to pre-serialize all the elements
    long listSize = 0;
    for (Bytes bytes : elements) {
      listSize += bytes.size();
      if (listSize > Integer.MAX_VALUE) {
        throw new IllegalArgumentException("Cannot serialize list: overall length is too large");
      }
    }
    appender.accept(encodeUInt32(listSize));
    for (Bytes bytes : elements) {
      encodeFixedBytesTo(bytes, appender);
    }
  }

  /**
   * Encode a list of strings.
   *
   * @param elements the strings to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeStringList(String... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length * 2 + 1);
    encodeStringListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of strings
   *
   * @param elements the list of strings to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeStringList(List<String> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() * 2 + 1);
    encodeStringListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeStringListTo(String[] elements, Consumer<Bytes> appender) {
    Bytes[] elementBytes = new Bytes[elements.length];
    for (int i = 0; i < elements.length; ++i) {
      elementBytes[i] = Bytes.wrap(elements[i].getBytes(UTF_8));
    }
    encodeBytesListTo(elementBytes, appender);
  }

  static void encodeStringListTo(List<String> elements, Consumer<Bytes> appender) {
    Bytes[] elementBytes = new Bytes[elements.size()];
    for (int i = 0; i < elements.size(); ++i) {
      elementBytes[i] = Bytes.wrap(elements.get(i).getBytes(UTF_8));
    }
    encodeBytesListTo(elementBytes, appender);
  }

  /**
   * Encode a list of two's compliment integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large for the specified {@code bitLength}
   */
  public static Bytes encodeIntList(int bitLength, int... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length + 1);
    encodeIntListTo(bitLength, elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of two's compliment integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the list of Integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large for the specified {@code bitLength}
   */
  public static Bytes encodeIntList(int bitLength, List<Integer> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() + 1);
    encodeIntListTo(bitLength, elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeIntListTo(int bitLength, int[] elements, Consumer<byte[]> appender) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    appender.accept(listLengthPrefix(elements.length, bitLength / 8));
    for (int value : elements) {
      appender.accept(encodeLongToByteArray(value, bitLength));
    }
  }

  static void encodeIntListTo(int bitLength, List<Integer> elements, Consumer<byte[]> appender) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    appender.accept(listLengthPrefix(elements.size(), bitLength / 8));
    for (int value : elements) {
      appender.accept(encodeLongToByteArray(value, bitLength));
    }
  }

  /**
   * Encode a list of two's compliment long integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large for the specified {@code bitLength}
   */
  public static Bytes encodeLongIntList(int bitLength, long... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length + 1);
    encodeLongIntListTo(bitLength, elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of two's compliment long integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the list of Longs to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large for the specified {@code bitLength}
   */
  public static Bytes encodeLongIntList(int bitLength, List<Long> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() + 1);
    encodeLongIntListTo(bitLength, elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeLongIntListTo(int bitLength, long[] elements, Consumer<byte[]> appender) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    appender.accept(listLengthPrefix(elements.length, bitLength / 8));
    for (long value : elements) {
      appender.accept(encodeLongToByteArray(value, bitLength));
    }
  }

  static void encodeLongIntListTo(int bitLength, List<Long> elements, Consumer<byte[]> appender) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    appender.accept(listLengthPrefix(elements.size(), bitLength / 8));
    for (long value : elements) {
      appender.accept(encodeLongToByteArray(value, bitLength));
    }
  }

  /**
   * Encode a list of big integers.
   *
   * @param bitLength The bit length of the encoded integers (must be a multiple of 8).
   * @param elements The integers to write.
   * @return SSZ encoding in a {@link Bytes} value.
   * @throws IllegalArgumentException If any values are too large for the specified {@code bitLength}.
   */
  public static Bytes encodeBigIntegerList(int bitLength, BigInteger... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length + 1);
    encodeBigIntegerListTo(bitLength, elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of big integers.
   *
   * @param bitLength The bit length of the encoded integers (must be a multiple of 8).
   * @param elements The list of BigIntegers to write.
   * @return SSZ encoding in a {@link Bytes} value.
   * @throws IllegalArgumentException If any values are too large for the specified {@code bitLength}.
   */
  public static Bytes encodeBigIntegerList(int bitLength, List<BigInteger> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() + 1);
    encodeBigIntegerListTo(bitLength, elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeBigIntegerListTo(int bitLength, BigInteger[] elements, Consumer<byte[]> appender) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    appender.accept(listLengthPrefix(elements.length, bitLength / 8));
    for (BigInteger value : elements) {
      appender.accept(encodeBigIntegerToByteArray(value, bitLength));
    }
  }

  static void encodeBigIntegerListTo(int bitLength, List<BigInteger> elements, Consumer<byte[]> appender) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    appender.accept(listLengthPrefix(elements.size(), bitLength / 8));
    for (BigInteger value : elements) {
      appender.accept(encodeBigIntegerToByteArray(value, bitLength));
    }
  }

  /**
   * Encode a list of 8-bit two's compliment integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  public static Bytes encodeInt8List(int... elements) {
    return encodeIntList(8, elements);
  }

  /**
   * Encode a list of 8-bit two's compliment integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  public static Bytes encodeInt8List(List<Integer> elements) {
    return encodeIntList(8, elements);
  }

  /**
   * Encode a list of 16-bit two's compliment integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  public static Bytes encodeInt16List(int... elements) {
    return encodeIntList(16, elements);
  }

  /**
   * Encode a list of 16-bit two's compliment integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  public static Bytes encodeInt16List(List<Integer> elements) {
    return encodeIntList(16, elements);
  }

  /**
   * Encode a list of 32-bit two's compliment integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeInt32List(int... elements) {
    return encodeIntList(32, elements);
  }

  /**
   * Encode a list of 32-bit two's compliment integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeInt32List(List<Integer> elements) {
    return encodeIntList(32, elements);
  }

  /**
   * Encode a list of 64-bit two's compliment integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeInt64List(long... elements) {
    return encodeLongIntList(64, elements);
  }

  /**
   * Encode a list of 64-bit two's compliment integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeInt64List(List<Long> elements) {
    return encodeLongIntList(64, elements);
  }

  /**
   * Encode a list of unsigned integers.
   *
   * Note that the {@code elements} are native signed ints, but will be interpreted as an unsigned values.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large for the specified {@code bitLength}
   */
  public static Bytes encodeUIntList(int bitLength, int... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length + 1);
    encodeUIntListTo(bitLength, elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of unsigned integers.
   *
   * Note that the {@code elements} are native signed ints, but will be interpreted as an unsigned values.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large for the specified {@code bitLength}
   */
  public static Bytes encodeUIntList(int bitLength, List<Integer> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() + 1);
    encodeUIntListTo(bitLength, elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeUIntListTo(int bitLength, int[] elements, Consumer<byte[]> appender) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    appender.accept(listLengthPrefix(elements.length, bitLength / 8));
    for (int value : elements) {
      appender.accept(encodeULongToByteArray(value, bitLength));
    }
  }

  static void encodeUIntListTo(int bitLength, List<Integer> elements, Consumer<byte[]> appender) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    appender.accept(listLengthPrefix(elements.size(), bitLength / 8));
    for (int value : elements) {
      appender.accept(encodeULongToByteArray(value, bitLength));
    }
  }

  /**
   * Encode a list of unsigned long integers.
   *
   * Note that the {@code elements} are native signed longs, but will be interpreted as an unsigned values.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large for the specified {@code bitLength}
   */
  public static Bytes encodeULongIntList(int bitLength, long... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length + 1);
    encodeULongIntListTo(bitLength, elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of unsigned long integers.
   *
   * Note that the {@code elements} are native signed longs, but will be interpreted as an unsigned values.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large for the specified {@code bitLength}
   */
  public static Bytes encodeULongIntList(int bitLength, List<Long> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() + 1);
    encodeULongIntListTo(bitLength, elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeULongIntListTo(int bitLength, long[] elements, Consumer<byte[]> appender) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    appender.accept(listLengthPrefix(elements.length, bitLength / 8));
    for (long value : elements) {
      appender.accept(encodeULongToByteArray(value, bitLength));
    }
  }

  static void encodeULongIntListTo(int bitLength, List<Long> elements, Consumer<byte[]> appender) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    appender.accept(listLengthPrefix(elements.size(), bitLength / 8));
    for (long value : elements) {
      appender.accept(encodeULongToByteArray(value, bitLength));
    }
  }

  /**
   * Encode a list of 8-bit unsigned integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  public static Bytes encodeUInt8List(int... elements) {
    return encodeUIntList(8, elements);
  }

  /**
   * Encode a list of 8-bit unsigned integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  public static Bytes encodeUInt8List(List<Integer> elements) {
    return encodeUIntList(8, elements);
  }

  /**
   * Encode a list of 16-bit unsigned integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  public static Bytes encodeUInt16List(int... elements) {
    return encodeUIntList(16, elements);
  }

  /**
   * Encode a list of 16-bit unsigned integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  public static Bytes encodeUInt16List(List<Integer> elements) {
    return encodeUIntList(16, elements);
  }

  /**
   * Encode a list of 32-bit unsigned integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large to be represented in 32 bits
   */
  public static Bytes encodeUInt32List(long... elements) {
    return encodeULongIntList(32, elements);
  }

  /**
   * Encode a list of 32-bit unsigned integers.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any values are too large to be represented in 32 bits
   */
  public static Bytes encodeUInt32List(List<Long> elements) {
    return encodeULongIntList(32, elements);
  }

  /**
   * Encode a list of 64-bit unsigned integers.
   *
   * Note that the {@code elements} are native signed longs, but will be interpreted as an unsigned values.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeUInt64List(long... elements) {
    return encodeULongIntList(64, elements);
  }

  /**
   * Encode a list of 64-bit unsigned integers.
   *
   * Note that the {@code elements} are native signed longs, but will be interpreted as an unsigned values.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeUInt64List(List<Long> elements) {
    return encodeULongIntList(64, elements);
  }

  /**
   * Encode a list of {@link UInt256}.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeUInt256List(UInt256... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length + 1);
    encodeUInt256ListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of {@link UInt256}.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeUInt256List(List<UInt256> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() + 1);
    encodeUInt256ListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeUInt256ListTo(UInt256[] elements, Consumer<Bytes> appender) {
    appender.accept(Bytes.wrap(listLengthPrefix(elements.length, 256 / 8)));
    for (UInt256 value : elements) {
      appender.accept(encodeUInt256(value));
    }
  }

  static void encodeUInt256ListTo(List<UInt256> elements, Consumer<Bytes> appender) {
    appender.accept(Bytes.wrap(listLengthPrefix(elements.size(), 256 / 8)));
    for (UInt256 value : elements) {
      appender.accept(encodeUInt256(value));
    }
  }

  /**
   * Encode a list of {@link UInt384}.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeUInt384List(UInt384... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length + 1);
    encodeUInt384ListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of {@link UInt384}.
   *
   * @param elements the integers to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeUInt384List(List<UInt384> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() + 1);
    encodeUInt384ListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeUInt384ListTo(UInt384[] elements, Consumer<Bytes> appender) {
    appender.accept(Bytes.wrap(listLengthPrefix(elements.length, 256 / 8)));
    for (UInt384 value : elements) {
      appender.accept(encodeUInt384(value));
    }
  }

  static void encodeUInt384ListTo(List<UInt384> elements, Consumer<Bytes> appender) {
    appender.accept(Bytes.wrap(listLengthPrefix(elements.size(), 256 / 8)));
    for (UInt384 value : elements) {
      appender.accept(encodeUInt384(value));
    }
  }

  /**
   * Encode a list of hashes.
   *
   * @param elements the hashes to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeHashList(Bytes... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length + 1);
    encodeHashListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of hashes.
   *
   * @param elements the hashes to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeHashList(List<? extends Bytes> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() + 1);
    encodeHashListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeHashListTo(Bytes[] elements, Consumer<Bytes> appender) {
    int hashLength = 0;
    for (Bytes bytes : elements) {
      if (hashLength == 0) {
        hashLength = bytes.size();
      } else {
        if (bytes.size() != hashLength) {
          throw new IllegalArgumentException("Hashes must be all of the same size");
        }
      }
    }
    appender.accept(Bytes.wrap(listLengthPrefix(elements.length, 32)));
    for (Bytes bytes : elements) {
      appender.accept(bytes);
    }
  }

  static void encodeHashListTo(List<? extends Bytes> elements, Consumer<Bytes> appender) {
    int hashLength = 0;
    for (Bytes bytes : elements) {
      if (hashLength == 0) {
        hashLength = bytes.size();
      } else {
        if (bytes.size() != hashLength) {
          throw new IllegalArgumentException("Hashes must be all of the same size");
        }
      }
    }
    appender.accept(Bytes.wrap(listLengthPrefix(elements.size(), 32)));
    for (Bytes bytes : elements) {
      appender.accept(bytes);
    }
  }

  /**
   * Encode a list of addresses.
   *
   * @param elements the addresses to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any {@code address.size != 20}
   */
  public static Bytes encodeAddressList(Bytes... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length + 1);
    encodeAddressListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of addresses.
   *
   * @param elements the addresses to write
   * @return SSZ encoding in a {@link Bytes} value
   * @throws IllegalArgumentException if any {@code address.size != 20}
   */
  public static Bytes encodeAddressList(List<? extends Bytes> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() + 1);
    encodeAddressListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeAddressListTo(Bytes[] elements, Consumer<Bytes> appender) {
    appender.accept(Bytes.wrap(listLengthPrefix(elements.length, 20)));
    for (Bytes bytes : elements) {
      appender.accept(encodeAddress(bytes));
    }
  }

  static void encodeAddressListTo(List<? extends Bytes> elements, Consumer<Bytes> appender) {
    appender.accept(Bytes.wrap(listLengthPrefix(elements.size(), 20)));
    for (Bytes bytes : elements) {
      appender.accept(encodeAddress(bytes));
    }
  }

  /**
   * Encode a list of booleans.
   *
   * @param elements the booleans to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeBooleanList(boolean... elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.length + 1);
    encodeBooleanListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  /**
   * Encode a list of booleans.
   *
   * @param elements the booleans to write
   * @return SSZ encoding in a {@link Bytes} value
   */
  public static Bytes encodeBooleanList(List<Boolean> elements) {
    ArrayList<Bytes> encoded = new ArrayList<>(elements.size() + 1);
    encodeBooleanListTo(elements, b -> encoded.add(Bytes.wrap(b)));
    return Bytes.wrap(encoded.toArray(new Bytes[0]));
  }

  static void encodeBooleanListTo(boolean[] elements, Consumer<Bytes> appender) {
    appender.accept(encodeInt32(elements.length));
    for (boolean value : elements) {
      appender.accept(encodeBoolean(value));
    }
  }

  static void encodeBooleanListTo(List<Boolean> elements, Consumer<Bytes> appender) {
    appender.accept(encodeInt32(elements.size()));
    for (boolean value : elements) {
      appender.accept(encodeBoolean(value));
    }
  }

  private static byte[] listLengthPrefix(long nElements, int elementBytes) {
    long listSize;
    try {
      listSize = Math.multiplyExact(nElements, (long) elementBytes);
    } catch (ArithmeticException e) {
      listSize = Long.MAX_VALUE;
    }
    if (listSize > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Cannot serialize list: overall length is too large");
    }
    return encodeLongToByteArray(listSize, 32);
  }


  // Decoding

  /**
   * Read and decode SSZ from a {@link Bytes} value.
   *
   * @param source the SSZ encoded bytes
   * @param fn a function that will be provided a {@link SSZReader}
   * @param <T> the result type of the reading function
   * @return the result from the reading function
   */
  public static <T> T decode(Bytes source, Function<SSZReader, T> fn) {
    requireNonNull(source);
    requireNonNull(fn);
    return fn.apply(new BytesSSZReader(source));
  }

  /**
   * Read a SSZ encoded bytes from a {@link Bytes} value.
   *
   * Note: prefer to use {@link #decodeBytes(Bytes, int)} instead, especially when reading untrusted data.
   *
   * @param source the SSZ encoded bytes
   * @return the bytes
   * @throws InvalidSSZTypeException if the next SSZ value is not a byte array, or is too large (greater than 2^32
   *         bytes)
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static Bytes decodeBytes(Bytes source) {
    return decode(source, SSZReader::readBytes);
  }

  /**
   * Read a SSZ encoded bytes from a {@link Bytes} value.
   *
   * @param source the SSZ encoded bytes
   * @param limit the maximum number of bytes to read
   * @return the bytes
   * @throws InvalidSSZTypeException if the next SSZ value is not a byte array, or would exceed the limit
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static Bytes decodeBytes(Bytes source, int limit) {
    return decode(source, r -> r.readBytes(limit));
  }

  /**
   * Read a SSZ encoded string from a {@link Bytes} value.
   *
   * Note: prefer to use {@link #decodeString(Bytes, int)} instead, especially when reading untrusted data.
   *
   * @param source the SSZ encoded bytes
   * @return a string
   * @throws InvalidSSZTypeException if the next SSZ value is not a byte array, or is too large (greater than 2^32
   *         bytes)
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static String decodeString(Bytes source) {
    return decode(source, SSZReader::readString);
  }

  /**
   * Read a SSZ encoded string from a {@link Bytes} value.
   *
   * @param source the SSZ encoded bytes
   * @param limit the maximum number of bytes to read
   * @return a string
   * @throws InvalidSSZTypeException if the next SSZ value is not a byte array, or would exceed the limit
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static String decodeString(Bytes source, int limit) {
    return decode(source, r -> r.readString(limit));
  }

  /**
   * Read a SSZ encoded two's-compliment integer from a {@link Bytes} value.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integer to read (a multiple of 8)
   * @return an int
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for the desired bit length, or the decoded
   *         value was too large to fit into an int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static int decodeInt(Bytes source, int bitLength) {
    return decode(source, r -> r.readInt(bitLength));
  }

  /**
   * Read a SSZ encoded two's-compliment long integer from a {@link Bytes} value.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integer to read (a multiple of 8)
   * @return a long
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for the desired bit length, or the decoded
   *         value was too large to fit into a long
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static long decodeLong(Bytes source, int bitLength) {
    return decode(source, r -> r.readLong(bitLength));
  }

  /**
   * Read a SSZ encoded two's-compliment big integer from a {@link Bytes} value.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integer to read (a multiple of 8)
   * @return a string
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for the desired bit length
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static BigInteger decodeBigInteger(Bytes source, int bitLength) {
    return decode(source, r -> r.readBigInteger(bitLength));
  }

  /**
   * Read an 8-bit two's-compliment integer from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return an int
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for an 8-bit int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static int decodeInt8(Bytes source) {
    return decodeInt(source, 8);
  }

  /**
   * Read a 16-bit two's-compliment integer from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return an int
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for a 16-bit int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static int decodeInt16(Bytes source) {
    return decodeInt(source, 16);
  }

  /**
   * Read a 32-bit two's-compliment integer from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return an int
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for a 32-bit int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static int decodeInt32(Bytes source) {
    return decodeInt(source, 32);
  }

  /**
   * Read a 64-bit two's-compliment integer from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return an int
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for a 64-bit int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static long decodeInt64(Bytes source) {
    return decodeLong(source, 64);
  }

  /**
   * Read a SSZ encoded unsigned integer from a {@link Bytes} value.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integers to read (a multiple of 8)
   * @return an unsigned int
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for the desired bit length, or the decoded
   *         value was too large to fit into an int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static int decodeUInt(Bytes source, int bitLength) {
    return decode(source, r -> r.readUInt(bitLength));
  }

  /**
   * Read a SSZ encoded unsigned long integer from a {@link Bytes} value.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integers to read (a multiple of 8)
   * @return an unsigned long
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for the desired bit length, or the decoded
   *         value was too large to fit into a long
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static long decodeULong(Bytes source, int bitLength) {
    return decode(source, r -> r.readULong(bitLength));
  }

  /**
   * Read a SSZ encoded unsigned big integer from a {@link Bytes} value.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integers to read (a multiple of 8)
   * @return a string
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for the desired bit length
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static BigInteger decodeUnsignedBigInteger(Bytes source, int bitLength) {
    return decode(source, r -> r.readBigInteger(bitLength));
  }

  /**
   * Read an 8-bit unsigned integer from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return an int
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for an 8-bit int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static int decodeUInt8(Bytes source) {
    return decodeUInt(source, 8);
  }

  /**
   * Read a 16-bit unsigned integer from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return an int
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for a 16-bit int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static int decodeUInt16(Bytes source) {
    return decodeUInt(source, 16);
  }

  /**
   * Read a 32-bit unsigned integer from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return an int
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for a 32-bit int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static long decodeUInt32(Bytes source) {
    return decodeULong(source, 32);
  }

  /**
   * Read a 64-bit unsigned integer from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return an int
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for a 64-bit int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static long decodeUInt64(Bytes source) {
    return decodeLong(source, 64);
  }

  /**
   * Read a 256-bit unsigned integer from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a {@link UInt256}
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for a 256-bit int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static UInt256 decodeUInt256(Bytes source) {
    return decode(source, SSZReader::readUInt256);
  }

  /**
   * Read a 384-bit unsigned integer from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a {@link UInt384}
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for a 384-bit int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static UInt384 decodeUInt384(Bytes source) {
    return decode(source, SSZReader::readUInt384);
  }

  /**
   * Read a boolean value from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a boolean
   * @throws InvalidSSZTypeException if the decoded value is not a boolean
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static boolean decodeBoolean(Bytes source) {
    return decode(source, SSZReader::readBoolean);
  }

  /**
   * Read a 20-byte address from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return the bytes of the Address
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for a 20-byte address
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static Bytes decodeAddress(Bytes source) {
    return decode(source, SSZReader::readAddress);
  }

  /**
   * Read a 32-byte hash from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @param hashLength the length of the hash (in bytes)
   * @return the bytes of the hash
   * @throws InvalidSSZTypeException if there are insufficient encoded bytes for a 32-byte hash
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static Bytes decodeHash(Bytes source, int hashLength) {
    return decode(source, r -> r.readHash(hashLength));
  }

  /**
   * Read a list of {@link Bytes} from the SSZ source.
   *
   * Note: prefer to use {@link #decodeBytesList(Bytes, int)} instead, especially when reading untrusted data.
   *
   * @param source the SSZ encoded bytes
   * @return a list of {@link Bytes}
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, any value in the list is not a byte array, or
   *         any byte array is too large (greater than 2^32 bytes)
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Bytes> decodeBytesList(Bytes source) {
    return decode(source, SSZReader::readBytesList);
  }

  /**
   * Read a list of {@link Bytes} from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @param limit the maximum number of bytes to read for each list element
   * @return a list of {@link Bytes}
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, any value in the list is not a byte array, or
   *         any byte array is too large (greater than 2^32 bytes)
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Bytes> decodeBytesList(Bytes source, int limit) {
    return decode(source, r -> r.readBytesList(limit));
  }

  /**
   * Read a list of byte arrays from the SSZ source.
   *
   * Note: prefer to use {@link #decodeByteArrayList(Bytes, int)} instead, especially when reading untrusted data.
   *
   * @param source the SSZ encoded bytes
   * @return a list of byte arrays
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, any value in the list is not a byte array, or
   *         any byte array is too large (greater than 2^32 bytes)
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<byte[]> decodeByteArrayList(Bytes source) {
    return decode(source, SSZReader::readByteArrayList);
  }

  /**
   * Read a list of byte arrays from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @param limit The maximum number of bytes to read for each list element.
   * @return a list of byte arrays
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, any value in the list is not a byte array, or
   *         any byte array is too large (greater than 2^32 bytes)
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<byte[]> decodeByteArrayList(Bytes source, int limit) {
    return decode(source, r -> r.readByteArrayList(limit));
  }

  /**
   * Read a list of strings from the SSZ source.
   *
   * Note: prefer to use {@link #decodeStringList(Bytes, int)} instead, especially when reading untrusted data.
   *
   * @param source the SSZ encoded bytes
   * @return a list of strings
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, any value in the list is not a string, or any
   *         string is too large (greater than 2^32 bytes)
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<String> decodeStringList(Bytes source) {
    return decode(source, SSZReader::readStringList);
  }

  /**
   * Read a list of strings from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @param limit The maximum number of bytes to read for each list element.
   * @return a list of strings
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, any value in the list is not a string, or any
   *         string is too large (greater than 2^32 bytes)
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<String> decodeStringList(Bytes source, int limit) {
    return decode(source, r -> r.readStringList(limit));
  }

  /**
   * Read a list of two's-compliment int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integers to read (a multiple of 8)
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Integer> decodeIntList(Bytes source, int bitLength) {
    return decode(source, r -> r.readIntList(bitLength));
  }

  /**
   * Read a list of two's-compliment long int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integers to read (a multiple of 8)
   * @return a list of longs
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into a long
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Long> decodeLongIntList(Bytes source, int bitLength) {
    return decode(source, r -> r.readLongIntList(bitLength));
  }

  /**
   * Read a list of two's-compliment big integer values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integers to read (a multiple of 8)
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, or there are insufficient encoded bytes for
   *         the desired bit length or any value in the list
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<BigInteger> decodeBigIntegerList(Bytes source, int bitLength) {
    return decode(source, r -> r.readBigIntegerList(bitLength));
  }

  /**
   * Read a list of 8-bit two's-compliment int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Integer> decodeInt8List(Bytes source) {
    return decode(source, SSZReader::readInt8List);
  }

  /**
   * Read a list of 16-bit two's-compliment int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Integer> decodeInt16List(Bytes source) {
    return decode(source, SSZReader::readInt16List);
  }

  /**
   * Read a list of 32-bit two's-compliment int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Integer> decodeInt32List(Bytes source) {
    return decode(source, SSZReader::readInt32List);
  }

  /**
   * Read a list of 64-bit two's-compliment int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Long> decodeInt64List(Bytes source) {
    return decode(source, SSZReader::readInt64List);
  }

  /**
   * Read a list of unsigned int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integers to read (a multiple of 8)
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Integer> decodeUIntList(Bytes source, int bitLength) {
    return decode(source, r -> r.readUIntList(bitLength));
  }

  /**
   * Read a list of unsigned long int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integers to read (a multiple of 8)
   * @return a list of longs
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into a long
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Long> decodeULongIntList(Bytes source, int bitLength) {
    return decode(source, r -> r.readULongIntList(bitLength));
  }

  /**
   * Read a list of unsigned big integer values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @param bitLength the bit length of the integers to read (a multiple of 8)
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, or there are insufficient encoded bytes for
   *         the desired bit length of any value in the list
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<BigInteger> decodeUnsignedBigIntegerList(Bytes source, int bitLength) {
    return decode(source, r -> r.readUnsignedBigIntegerList(bitLength));
  }

  /**
   * Read a list of 8-bit unsigned int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length of any value in the list, or any decoded value was too large to fit into an int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Integer> decodeUInt8List(Bytes source) {
    return decode(source, SSZReader::readUInt8List);
  }

  /**
   * Read a list of 16-bit unsigned int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length of any value in the list, or any decoded value was too large to fit into an int
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Integer> decodeUInt16List(Bytes source) {
    return decode(source, SSZReader::readUInt16List);
  }

  /**
   * Read a list of 32-bit unsigned int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length of any value in the list, or any decoded value was too large to fit into a long
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Long> decodeUInt32List(Bytes source) {
    return decode(source, SSZReader::readUInt32List);
  }

  /**
   * Read a list of 64-bit unsigned int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of ints
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length of any value in the list, or any decoded value was too large to fit into a long
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Long> decodeUInt64List(Bytes source) {
    return decode(source, SSZReader::readUInt64List);
  }

  /**
   * Read a list of 256-bit unsigned int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of {@link UInt256}
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length of any value in the list, or any decoded value was too large to fit into {@link UInt256}
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<UInt256> decodeUInt256List(Bytes source) {
    return decode(source, SSZReader::readUInt256List);
  }

  /**
   * Read a list of 384-bit unsigned int values from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of {@link UInt384}
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length of any value in the list, or any decoded value was too large to fit into a
   *         {@link UInt384}
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<UInt384> decodeUInt384List(Bytes source) {
    return decode(source, SSZReader::readUInt384List);
  }

  /**
   * Read a list of 20-byte addresses from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of 20-byte addresses
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for any
   *         address in the list
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Bytes> decodeAddressList(Bytes source) {
    return decode(source, SSZReader::readAddressList);
  }

  /**
   * Read a list of 32-byte hashes from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @param hashLength The length of the hash (in bytes).
   * @return a list of 32-byte hashes
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for any
   *         hash in the list
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Bytes> decodeHashList(Bytes source, int hashLength) {
    return decode(source, r -> r.readHashList(hashLength));
  }

  /**
   * Read a list of booleans from the SSZ source.
   *
   * @param source the SSZ encoded bytes
   * @return a list of booleans
   * @throws InvalidSSZTypeException if the next SSZ value is not a list, there are insufficient encoded bytes for all
   *         the booleans in the list
   * @throws EndOfSSZException if there are no more SSZ values to read
   */
  public static List<Boolean> decodeBooleanList(Bytes source) {
    return decode(source, SSZReader::readBooleanList);
  }
}
