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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.bigints.UInt384;

import java.math.BigInteger;
import java.util.List;

/**
 * A writer for encoding values to SSZ.
 */
public interface SSZWriter {

  /**
   * Append an already SSZ encoded value.
   *
   * Note that this method <b>may not</b> validate that {@code value} is a valid SSZ sequence. Appending an invalid SSZ
   * sequence will cause the entire SSZ encoding produced by this writer to also be invalid.
   *
   * @param value the SSZ encoded bytes to append
   */
  void writeSSZ(Bytes value);

  /**
   * Append an already SSZ encoded value.
   *
   * Note that this method <b>may not</b> validate that {@code value} is a valid SSZ sequence. Appending an invalid SSZ
   * sequence will cause the entire SSZ encoding produced by this writer to also be invalid.
   *
   * @param value the SSZ encoded bytes to append
   */
  default void writeSSZ(byte[] value) {
    writeSSZ(Bytes.wrap(value));
  }

  /**
   * Encode a {@link Bytes} value to SSZ.
   *
   * @param value the byte array to encode
   */
  default void writeBytes(Bytes value) {
    SSZ.encodeBytesTo(value, this::writeSSZ);
  }

  /**
   * Encode a byte array to SSZ.
   *
   * @param value the byte array to encode
   */
  default void writeBytes(byte[] value) {
    SSZ.encodeByteArrayTo(value, this::writeSSZ);
  }

  /**
   * Encode a known fixed-length {@link Bytes} value to SSZ without the length mixin.
   *
   * @param value the byte array to encode
   * @throws IllegalArgumentException if the byteLength is not the same size as value.
   */
  default void writeFixedBytes(Bytes value) {
    SSZ.encodeFixedBytesTo(value, this::writeSSZ);
  }

  /**
   * Write a string to the output.
   *
   * @param str the string to write
   */
  default void writeString(String str) {
    SSZ.encodeStringTo(str, this::writeSSZ);
  }

  /**
   * Write a two's-compliment integer to the output.
   *
   * @param value the integer to write
   * @param bitLength the bit length of the integer value
   * @throws IllegalArgumentException if the value is too large for the specified bit length
   */
  default void writeInt(int value, int bitLength) {
    writeSSZ(SSZ.encodeLongToByteArray(value, bitLength));
  }

  /**
   * Write a two's-compliment long to the output.
   *
   * @param value the long value to write
   * @param bitLength the bit length of the integer value
   * @throws IllegalArgumentException if the value is too large for the specified bit length
   */
  default void writeLong(long value, int bitLength) {
    writeSSZ(SSZ.encodeLongToByteArray(value, bitLength));
  }

  /**
   * Write a big integer to the output.
   *
   * @param value the integer to write
   * @param bitLength the bit length of the integer value
   * @throws IllegalArgumentException if the value is too large for the specified bit length
   */
  default void writeBigInteger(BigInteger value, int bitLength) {
    writeSSZ(SSZ.encodeBigIntegerToByteArray(value, bitLength));
  }

  /**
   * Write an 8-bit two's-compliment integer to the output.
   *
   * @param value the integer to write
   * @throws IllegalArgumentException if the value is too large to be represented in 8 bits
   */
  default void writeInt8(int value) {
    writeInt(value, 8);
  }

  /**
   * Write a 16-bit two's-compliment integer to the output.
   *
   * @param value the integer to write
   * @throws IllegalArgumentException If the value is too large to be represented in 16 bits
   */
  default void writeInt16(int value) {
    writeInt(value, 16);
  }

  /**
   * Write a 32-bit two's-compliment integer to the output.
   *
   * @param value the integer to write
   */
  default void writeInt32(int value) {
    writeInt(value, 32);
  }

  /**
   * Write a 64-bit two's-compliment integer to the output.
   *
   * @param value the long to write
   */
  default void writeInt64(long value) {
    writeLong(value, 64);
  }

  /**
   * Write an unsigned integer to the output.
   *
   * Note that the argument {@code value} is a native signed int but will be interpreted as an unsigned value.
   *
   * @param value the integer to write
   * @param bitLength the bit length of the integer value
   * @throws IllegalArgumentException if the value is too large for the specified bit length
   */
  default void writeUInt(int value, int bitLength) {
    writeSSZ(SSZ.encodeULongToByteArray(value, bitLength));
  }

  /**
   * Write an unsigned long to the output.
   *
   * Note that the argument {@code value} is a native signed long but will be interpreted as an unsigned value.
   *
   * @param value the long value to write
   * @param bitLength the bit length of the integer value
   * @throws IllegalArgumentException if the value is too large for the specified bit length
   */
  default void writeULong(long value, int bitLength) {
    writeSSZ(SSZ.encodeULongToByteArray(value, bitLength));
  }

  /**
   * Write an unsigned big integer to the output.
   *
   * @param value the integer to write
   * @param bitLength the bit length of the integer value
   * @throws IllegalArgumentException if the value is too large for the specified bit length or the value is negative
   */
  default void writeUBigInteger(BigInteger value, int bitLength) {
    writeSSZ(SSZ.encodeUBigIntegerToByteArray(value, bitLength));
  }

  /**
   * Write an 8-bit unsigned integer to the output.
   *
   * @param value the integer to write
   * @throws IllegalArgumentException if the value is too large to be represented in 8 bits
   */
  default void writeUInt8(int value) {
    writeUInt(value, 8);
  }

  /**
   * Write a 16-bit unsigned integer to the output.
   *
   * @param value the integer to write
   * @throws IllegalArgumentException If the value is too large to be represented in 16 bits
   */
  default void writeUInt16(int value) {
    writeUInt(value, 16);
  }

  /**
   * Write a 32-bit unsigned integer to the output.
   *
   * @param value the integer to write
   */
  default void writeUInt32(long value) {
    writeULong(value, 32);
  }

  /**
   * Write a 64-bit unsigned integer to the output.
   *
   * Note that the argument {@code value} is a native signed long but will be interpreted as an unsigned value.
   *
   * @param value the long to write
   */
  default void writeUInt64(long value) {
    writeULong(value, 64);
  }

  /**
   * Write a {@link UInt256} to the output.
   *
   * @param value the {@link UInt256} to write
   */
  default void writeUInt256(UInt256 value) {
    writeSSZ(SSZ.encodeUInt256(value));
  }

  /**
   * Write a {@link UInt384} to the output.
   *
   * @param value the {@link UInt384} to write
   */
  default void writeUInt384(UInt384 value) {
    writeSSZ(SSZ.encodeUInt384(value));
  }

  /**
   * Write a boolean to the output.
   *
   * @param value the boolean value
   */
  default void writeBoolean(boolean value) {
    writeSSZ(SSZ.encodeBoolean(value));
  }

  /**
   * Write an address.
   *
   * @param address the address (must be exactly 20 bytes)
   * @throws IllegalArgumentException if {@code address.size != 20}
   */
  default void writeAddress(Bytes address) {
    writeSSZ(SSZ.encodeAddress(address));
  }

  /**
   * Write a hash.
   *
   * @param hash the hash
   */
  default void writeHash(Bytes hash) {
    writeSSZ(SSZ.encodeHash(hash));
  }

  /**
   * Write a list of bytes.
   *
   * @param elements the bytes to write as a list
   */
  default void writeBytesList(Bytes... elements) {
    SSZ.encodeBytesListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of bytes.
   *
   * @param elements the bytes to write as a list
   */
  default void writeBytesList(List<? extends Bytes> elements) {
    SSZ.encodeBytesListTo(elements, this::writeSSZ);
  }

  /**
   * Write a vector of bytes.
   *
   * @param elements the bytes to write as a list
   */
  default void writeVector(List<? extends Bytes> elements) {
    SSZ.encodeBytesVectorTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of known-size homogenous bytes. The list itself WILL have a length mixin, but the elements WILL NOT.
   *
   * @param elements the known-size bytes to write as a list
   */
  default void writeFixedBytesList(List<? extends Bytes> elements) {
    SSZ.encodeFixedBytesListTo(elements, this::writeSSZ);
  }

  /**
   * Write a known-size fixed-length list of known-size homogenous bytes. Neither the list nor the elements in the list
   * will have a length mixin.
   *
   * @param elements the bytes to write as a list
   */
  default void writeFixedBytesVector(List<? extends Bytes> elements) {
    SSZ.encodeFixedBytesVectorTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of strings, which must be of the same length
   *
   * @param elements the strings to write as a list
   */
  default void writeStringList(String... elements) {
    SSZ.encodeStringListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of strings, which must be of the same length
   *
   * @param elements the strings to write as a list
   */
  default void writeStringList(List<String> elements) {
    SSZ.encodeStringListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of two's compliment integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  default void writeIntList(int bitLength, int... elements) {
    SSZ.encodeIntListTo(bitLength, elements, this::writeSSZ);
  }

  /**
   * Write a list of two's compliment integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  default void writeIntList(int bitLength, List<Integer> elements) {
    SSZ.encodeIntListTo(bitLength, elements, this::writeSSZ);
  }

  /**
   * Write a list of two's compliment long integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the long integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  default void writeLongIntList(int bitLength, long... elements) {
    SSZ.encodeLongIntListTo(bitLength, elements, this::writeSSZ);
  }

  /**
   * Write a list of two's compliment long integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the long integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  default void writeLongIntList(int bitLength, List<Long> elements) {
    SSZ.encodeLongIntListTo(bitLength, elements, this::writeSSZ);
  }

  /**
   * Write a list of big integers.
   *
   * @param bitLength the bit length of each integer
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if an integer cannot be stored in the number of bytes provided
   */
  default void writeBigIntegerList(int bitLength, BigInteger... elements) {
    SSZ.encodeBigIntegerListTo(bitLength, elements, this::writeSSZ);
  }

  /**
   * Write a list of big integers.
   *
   * @param bitLength the bit length of each integer
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if an integer cannot be stored in the number of bytes provided
   */
  default void writeBigIntegerList(int bitLength, List<BigInteger> elements) {
    SSZ.encodeBigIntegerListTo(bitLength, elements, this::writeSSZ);
  }

  /**
   * Write a list of 8-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  default void writeInt8List(int... elements) {
    writeIntList(8, elements);
  }

  /**
   * Write a list of 8-bit two's compliment integers.
   *
   * @param elements the integers to write as a list.
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  default void writeInt8List(List<Integer> elements) {
    writeIntList(8, elements);
  }

  /**
   * Write a list of 16-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  default void writeInt16List(int... elements) {
    writeIntList(16, elements);
  }

  /**
   * Write a list of 16-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  default void writeInt16List(List<Integer> elements) {
    writeIntList(16, elements);
  }

  /**
   * Write a list of 32-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   */
  default void writeInt32List(int... elements) {
    writeIntList(32, elements);
  }

  /**
   * Write a list of 32-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   */
  default void writeInt32List(List<Integer> elements) {
    writeIntList(32, elements);
  }

  /**
   * Write a list of 64-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   */
  default void writeInt64List(long... elements) {
    writeLongIntList(64, elements);
  }

  /**
   * Write a list of 64-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   */
  default void writeInt64List(List<Long> elements) {
    writeLongIntList(64, elements);
  }

  /**
   * Write a list of unsigned integers.
   *
   * Note that the {@code elements} are native signed ints, but will be interpreted as an unsigned values.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  default void writeUIntList(int bitLength, int... elements) {
    SSZ.encodeUIntListTo(bitLength, elements, this::writeSSZ);
  }

  /**
   * Write a list of unsigned integers.
   *
   * Note that the {@code elements} are native signed ints, but will be interpreted as an unsigned values.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  default void writeUIntList(int bitLength, List<Integer> elements) {
    SSZ.encodeUIntListTo(bitLength, elements, this::writeSSZ);
  }

  /**
   * Write a list of unsigned long integers.
   *
   * Note that the {@code elements} are native signed longs, but will be interpreted as an unsigned values.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the long integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  default void writeULongIntList(int bitLength, long... elements) {
    SSZ.encodeULongIntListTo(bitLength, elements, this::writeSSZ);
  }

  /**
   * Write a list of unsigned long integers.
   *
   * Note that the {@code elements} are native signed longs, but will be interpreted as an unsigned values.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the long integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  default void writeULongIntList(int bitLength, List<Long> elements) {
    SSZ.encodeULongIntListTo(bitLength, elements, this::writeSSZ);
  }

  /**
   * Write a list of 8-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  default void writeUInt8List(int... elements) {
    writeUIntList(8, elements);
  }

  /**
   * Write a list of 8-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  default void writeUInt8List(List<Integer> elements) {
    writeUIntList(8, elements);
  }

  /**
   * Write a list of 16-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  default void writeUInt16List(int... elements) {
    writeUIntList(16, elements);
  }

  /**
   * Write a list of 16-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  default void writeUInt16List(List<Integer> elements) {
    writeUIntList(16, elements);
  }

  /**
   * Write a list of 32-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 32 bits
   */
  default void writeUInt32List(long... elements) {
    writeULongIntList(32, elements);
  }

  /**
   * Write a list of 32-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 32 bits
   */
  default void writeUInt32List(List<Long> elements) {
    writeULongIntList(32, elements);
  }

  /**
   * Write a list of 64-bit unsigned integers.
   *
   * Note that the {@code elements} are native signed longs, but will be interpreted as an unsigned values.
   *
   * @param elements the integers to write as a list
   */
  default void writeUInt64List(long... elements) {
    writeULongIntList(64, elements);
  }

  /**
   * Write a list of 64-bit unsigned integers.
   *
   * Note that the {@code elements} are native signed longs, but will be interpreted as an unsigned values.
   *
   * @param elements the integers to write as a list
   */
  default void writeUInt64List(List<Long> elements) {
    writeULongIntList(64, elements);
  }

  /**
   * Write a list of unsigned 256-bit integers.
   *
   * @param elements the integers to write as a list
   */
  default void writeUInt256List(UInt256... elements) {
    SSZ.encodeUInt256ListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of unsigned 256-bit integers.
   *
   * @param elements the integers to write as a list
   */
  default void writeUInt256List(List<UInt256> elements) {
    SSZ.encodeUInt256ListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of unsigned 384-bit integers.
   *
   * @param elements the integers to write as a list
   */
  default void writeUInt384List(List<UInt384> elements) {
    SSZ.encodeUInt384ListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of unsigned 384-bit integers.
   *
   * @param elements the integers to write as a list
   */
  default void writeUInt384List(UInt384... elements) {
    SSZ.encodeUInt384ListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of hashes.
   *
   * @param elements the hashes to write as a list
   */
  default void writeHashList(Bytes... elements) {
    SSZ.encodeHashListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of hashes.
   *
   * @param elements the hashes to write as a list
   */
  default void writeHashList(List<? extends Bytes> elements) {
    SSZ.encodeHashListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of addresses.
   *
   * @param elements the addresses to write as a list
   * @throws IllegalArgumentException if any {@code address.size != 20}
   */
  default void writeAddressList(Bytes... elements) {
    SSZ.encodeAddressListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of addresses.
   *
   * @param elements the addresses to write as a list
   * @throws IllegalArgumentException if any {@code address.size != 20}
   */
  default void writeAddressList(List<? extends Bytes> elements) {
    SSZ.encodeAddressListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of booleans.
   *
   * @param elements the booleans to write as a list
   */
  default void writeBooleanList(boolean... elements) {
    SSZ.encodeBooleanListTo(elements, this::writeSSZ);
  }

  /**
   * Write a list of booleans.
   *
   * @param elements the booleans to write as a list
   */
  default void writeBooleanList(List<Boolean> elements) {
    SSZ.encodeBooleanListTo(elements, this::writeSSZ);
  }
}
