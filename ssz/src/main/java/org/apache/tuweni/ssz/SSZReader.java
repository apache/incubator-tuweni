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

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.bigints.UInt384;

import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A reader for consuming values from an SSZ encoded source.
 */
public interface SSZReader {

  /**
   * Read bytes from the SSZ source.
   *
   * Note: prefer to use {@link #readBytes(int)} instead, especially when reading untrusted data.
   *
   * @return The bytes for the next value.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or is too large (greater than 2^32
   *         bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default Bytes readBytes() {
    return readBytes(Integer.MAX_VALUE);
  }

  /**
   * Read bytes from the SSZ source.
   *
   * @param limit The maximum number of bytes to read.
   * @return The bytes for the next value.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  Bytes readBytes(int limit);

  /**
   * Read a known size fixed-length bytes value from the SSZ source.
   *
   * Note: prefer to use {@link #readFixedBytes(int, int)} instead, especially when reading untrusted data.
   *
   * @param byteLength The number of fixed-length Bytes (no length mixin) to read.
   * @return The bytes for the next value.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or is too large (greater than 2^32
   *         bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default Bytes readFixedBytes(int byteLength) {
    return readFixedBytes(byteLength, Integer.MAX_VALUE);
  }

  /**
   * Read a known size fixed-length bytes value from the SSZ source.
   *
   * @param byteLength The number of fixed-length Bytes (no length mixin) to read.
   * @param limit The maximum number of bytes to read.
   * @return The bytes for the next value.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  Bytes readFixedBytes(int byteLength, int limit);

  /**
   * Read a byte array from the SSZ source.
   *
   * Note: prefer to use {@link #readByteArray(int)} instead, especially when reading untrusted data.
   *
   * @return The byte array for the next value.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or is too large (greater than 2^32
   *         bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default byte[] readByteArray() {
    return readByteArray(Integer.MAX_VALUE);
  }

  /**
   * Read a byte array from the SSZ source.
   *
   * @param limit The maximum number of bytes to read.
   * @return The byte array for the next value.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default byte[] readByteArray(int limit) {
    return readBytes(limit).toArrayUnsafe();
  }

  /**
   * Read an array of fixed-length homogenous Bytes from the SSZ source.
   *
   * @param byteLength The number of fixed-length Bytes per array element.
   * @param limit The maximum number of bytes to read.
   * @return The byte array for the next value.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default byte[] readFixedByteArray(int byteLength, int limit) {
    return readFixedBytes(byteLength, limit).toArrayUnsafe();
  }

  /**
   * Read a string value from the SSZ source.
   *
   * Note: prefer to use {@link #readString(int)} instead, especially when reading untrusted data.
   *
   * @return A string.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or is too large (greater than 2^32
   *         bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default String readString() {
    return new String(readByteArray(), UTF_8);
  }

  /**
   * Read a string value from the SSZ source.
   *
   * @param limit The maximum number of bytes to read.
   * @return A string.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default String readString(int limit) {
    return new String(readByteArray(limit), UTF_8);
  }

  /**
   * Read a two's-compliment int value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length, or the decoded
   *         value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  int readInt(int bitLength);

  /**
   * Read a two's-compliment long value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return A long.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length, or the decoded
   *         value was too large to fit into a long.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  long readLong(int bitLength);

  /**
   * Read a big integer value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return A big integer.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  BigInteger readBigInteger(int bitLength);

  /**
   * Read an 8-bit two's-compliment integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for an 8-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default int readInt8() {
    return readInt(8);
  }

  /**
   * Read a 16-bit two's-compliment integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 16-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default int readInt16() {
    return readInt(16);
  }

  /**
   * Read a 32-bit two's-compliment integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 32-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default int readInt32() {
    return readInt(32);
  }

  /**
   * Read a 64-bit two's-compliment integer from the SSZ source.
   *
   * @return A long.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 64-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default long readInt64() {
    return readLong(64);
  }

  /**
   * Read an unsigned int value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return An unsigned int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length, or the decoded
   *         value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default int readUInt(int bitLength) {
    // encoding is the same for unsigned
    return readInt(bitLength);
  }

  /**
   * Read an unsigned long value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return An unsigned long.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length, or the decoded
   *         value was too large to fit into a long.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default long readULong(int bitLength) {
    // encoding is the same for unsigned
    return readLong(bitLength);
  }

  /**
   * Read an unsigned big integer value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return A big integer.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  BigInteger readUnsignedBigInteger(int bitLength);

  /**
   * Read an 8-bit unsigned integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for an 8-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default int readUInt8() {
    return readUInt(8);
  }

  /**
   * Read a 16-bit unsigned integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 16-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default int readUInt16() {
    return readUInt(16);
  }

  /**
   * Read a 32-bit unsigned integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 32-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default long readUInt32() {
    return readULong(32);
  }

  /**
   * Read a 64-bit unsigned integer from the SSZ source.
   *
   * @return A long.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 64-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default long readUInt64() {
    return readULong(64);
  }

  /**
   * Read a {@link UInt256} from the SSZ source.
   *
   * @return A {@link UInt256}.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 256-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  UInt256 readUInt256();

  /**
   * Read a {@link UInt384} from the SSZ source.
   *
   * @return A {@link UInt384}.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 384-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  UInt384 readUInt384();

  /**
   * Read a boolean from the SSZ source.
   *
   * @return A boolean.
   * @throws InvalidSSZTypeException If the decoded value is not a boolean.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default boolean readBoolean() {
    int value = readInt(8);
    if (value == 0) {
      return false;
    } else if (value == 1) {
      return true;
    } else {
      throw new InvalidSSZTypeException("decoded value is not a boolean");
    }
  }

  /**
   * Read a 20-byte address from the SSZ source.
   *
   * @return The bytes of the Address.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 20-byte address.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  Bytes readAddress();

  /**
   * Read a hash from the SSZ source.
   *
   * @param hashLength The length of the hash (in bytes).
   * @return The bytes of the hash.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 32-byte hash.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  Bytes readHash(int hashLength);

  /**
   * Read a list of {@link Bytes} from the SSZ source.
   *
   * Note: prefer to use {@link #readBytesList(int)} instead, especially when reading untrusted data.
   *
   * @return A list of {@link Bytes}.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   *         any byte array is too large (greater than 2^32 bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Bytes> readBytesList() {
    return readBytesList(Integer.MAX_VALUE);
  }

  /**
   * Read a known-size fixed-length list of {@link Bytes} from the SSZ source. The list WILL NOT have a length mixin,
   * where as the elements WILL.
   *
   * Note: prefer to use {@link #readVector(long, int)} instead, especially when reading untrusted data.
   *
   * @param listSize The size of the fixed-length list being read.
   * @return A list of {@link Bytes}.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   *         any byte array is too large (greater than 2^32 bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Bytes> readVector(long listSize) {
    return readVector(listSize, Integer.MAX_VALUE);
  }

  /**
   * Read a known-size fixed length list of known-size fixed length {@link Bytes} from the SSZ source.
   *
   * @param listSize The size of the fixed-length list being read.
   * @param byteLength The number of fixed-length Bytes per homogenous List element.
   * @param limit The maximum number of bytes to read for each list element.
   * @return A list of {@link Bytes}.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   *         the size of any byte array would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<Bytes> readFixedBytesVector(int listSize, int byteLength, int limit);

  /**
   * Read a known-size fixed length list of known-size fixed length {@link Bytes} from the SSZ source.
   *
   * Note: prefer to use {@link #readFixedBytesVector(int, int, int)} instead, especially when reading untrusted data.
   *
   * @param listSize The size of the fixed-length list being read.
   * @param byteLength The number of fixed-length Bytes per homogenous List element.
   * @return A list of {@link Bytes}.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   *         any byte array is too large (greater than 2^32 bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Bytes> readFixedBytesVector(int listSize, int byteLength) {
    return readFixedBytesVector(listSize, byteLength, Integer.MAX_VALUE);
  }

  /**
   * Read a list of known-size fixed length {@link Bytes} from the SSZ source. A length mixin IS expected for the list,
   * but IS NOT expected for the list elements.
   *
   * @param byteLength The number of fixed-length Bytes per homogenous List element.
   * @param limit The maximum number of bytes to read for each list element.
   * @return A list of {@link Bytes}.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   *         the size of any byte array would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<Bytes> readFixedBytesList(int byteLength, int limit);

  /**
   * Read a variable-length list of known-size fixed length {@link Bytes} from the SSZ source.
   *
   * Note: prefer to use {@link #readFixedBytesList(int, int)} instead, especially when reading untrusted data.
   *
   * @param byteLength The number of fixed-length Bytes per homogenous List element.
   * @return A list of {@link Bytes}.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   *         any byte array is too large (greater than 2^32 bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Bytes> readFixedBytesList(int byteLength) {
    return readFixedBytesList(byteLength, Integer.MAX_VALUE);
  }

  /**
   * Read a list of {@link Bytes} from the SSZ source.
   *
   * @param limit The maximum number of bytes to read for each list element.
   * @return A list of {@link Bytes}.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   *         the size of any byte array would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<Bytes> readBytesList(int limit);

  /**
   * Read a known-size fixed-length list of {@link Bytes} from the SSZ source.
   *
   * @param listSize The size of the fixed-length list being read.
   * @param limit The maximum number of bytes to read for each list element.
   * @return A list of {@link Bytes}.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   *         the size of any byte array would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<Bytes> readVector(long listSize, int limit);

  /**
   * Read a list of byte arrays from the SSZ source.
   *
   * Note: prefer to use {@link #readByteArrayList(int)} instead, especially when reading untrusted data.
   *
   * @return A list of byte arrays.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   *         any byte array is too large (greater than 2^32 bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<byte[]> readByteArrayList() {
    return readByteArrayList(Integer.MAX_VALUE);
  }

  /**
   * Read a list of byte arrays from the SSZ source.
   *
   * @param limit The maximum number of bytes to read for each list element.
   * @return A list of byte arrays.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   *         the size of any byte array would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<byte[]> readByteArrayList(int limit) {
    return readBytesList(limit).stream().map(Bytes::toArrayUnsafe).collect(Collectors.toList());
  }

  /**
   * Read a list of strings from the SSZ source.
   *
   * Note: prefer to use {@link #readStringList(int)} instead, especially when reading untrusted data.
   *
   * @return A list of strings.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a string, or any
   *         string is too large (greater than 2^32 bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<String> readStringList() {
    return readStringList(Integer.MAX_VALUE);
  }

  /**
   * Read a list of strings from the SSZ source.
   *
   * @param limit The maximum number of bytes to read for each list element.
   * @return A list of strings.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a string, or the
   *         size of any string would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<String> readStringList(int limit);

  /**
   * Read a list of two's-compliment int values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<Integer> readIntList(int bitLength);

  /**
   * Read a list of two's-compliment long int values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of longs.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into a long.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<Long> readLongIntList(int bitLength);

  /**
   * Read a list of two's-compliment big integer values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of big integers.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, or there are insufficient encoded bytes for
   *         the desired bit length or any value in the list.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<BigInteger> readBigIntegerList(int bitLength);

  /**
   * Read a list of 8-bit two's-compliment int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Integer> readInt8List() {
    return readIntList(8);
  }

  /**
   * Read a list of 16-bit two's-compliment int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Integer> readInt16List() {
    return readIntList(16);
  }

  /**
   * Read a list of 32-bit two's-compliment int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Integer> readInt32List() {
    return readIntList(32);
  }

  /**
   * Read a list of 64-bit two's-compliment int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Long> readInt64List() {
    return readLongIntList(64);
  }

  /**
   * Read a list of unsigned int values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Integer> readUIntList(int bitLength) {
    // encoding is the same for unsigned
    return readIntList(bitLength);
  }

  /**
   * Read a list of unsigned long int values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of longs.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into a long.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Long> readULongIntList(int bitLength) {
    // encoding is the same for unsigned
    return readLongIntList(bitLength);
  }

  /**
   * Read a list of unsigned big integer values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of big integers.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, or there are insufficient encoded bytes for
   *         the desired bit length or any value in the list.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<BigInteger> readUnsignedBigIntegerList(int bitLength);

  /**
   * Read a list of 8-bit unsigned int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Integer> readUInt8List() {
    return readUIntList(8);
  }

  /**
   * Read a list of 16-bit unsigned int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Integer> readUInt16List() {
    return readUIntList(16);
  }

  /**
   * Read a list of 32-bit unsigned int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Long> readUInt32List() {
    return readULongIntList(32);
  }

  /**
   * Read a list of 64-bit unsigned int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  default List<Long> readUInt64List() {
    return readULongIntList(64);
  }

  /**
   * Read a list of 256-bit unsigned int values from the SSZ source.
   *
   * @return A list of {@link UInt256}.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<UInt256> readUInt256List();

  /**
   * Read a list of 384-bit unsigned int values from the SSZ source.
   *
   * @return A list of {@link UInt384}.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   *         desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<UInt384> readUInt384List();

  /**
   * Read a list of 20-byte addresses from the SSZ source.
   *
   * @return A list of 20-byte addresses.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for any
   *         address in the list.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<Bytes> readAddressList();

  /**
   * Read a list of hashes from the SSZ source.
   *
   * @param hashLength The length of the hash (in bytes).
   * @return A list of hashes.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for any
   *         hash in the list.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<Bytes> readHashList(int hashLength);

  /**
   * Read a list of booleans from the SSZ source.
   *
   * @return A list of booleans.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for all
   *         the booleans in the list.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  List<Boolean> readBooleanList();

  /**
   * Check if all values have been read.
   *
   * @return {@code true} if all values have been read.
   */
  boolean isComplete();
}
