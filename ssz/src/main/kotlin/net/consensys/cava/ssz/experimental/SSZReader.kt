/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.ssz.experimental

import net.consensys.cava.bytes.Bytes
import net.consensys.cava.ssz.EndOfSSZException
import net.consensys.cava.ssz.InvalidSSZTypeException
import net.consensys.cava.units.bigints.UInt256
import net.consensys.cava.units.bigints.UInt384
import java.math.BigInteger
import java.nio.charset.StandardCharsets.UTF_8

/**
 * A reader for consuming values from an SSZ encoded source.
 */
// Does not extend net.consensys.cava.ssz.SSZReader (unlike SSZWriter) as the return types vary for the UInt methods.
@ExperimentalUnsignedTypes
interface SSZReader {

  /**
   * Read bytes from the SSZ source.
   *
   * @param limit The maximum number of bytes to read.
   * @return The bytes for the next value.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readBytes(limit: Int): Bytes

  /**
   * Read a byte array from the SSZ source.
   *
   * @param limit The maximum number of bytes to read.
   * @return The byte array for the next value.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readByteArray(limit: Int = Integer.MAX_VALUE): ByteArray = readBytes(limit).toArrayUnsafe()

  /**
   * Read a string value from the SSZ source.
   *
   * @param limit The maximum number of bytes to read.
   * @return A string.
   * @throws InvalidSSZTypeException If the next SSZ value is not a byte array, or would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readString(limit: Int): String = String(readByteArray(limit), UTF_8)

  /**
   * Read a two's-compliment int value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length, or the decoded
   * value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readInt(bitLength: Int): Int

  /**
   * Read a two's-compliment long value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return A long.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length, or the decoded
   * value was too large to fit into a long.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readLong(bitLength: Int): Long

  /**
   * Read a big integer value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return A big integer.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readBigInteger(bitLength: Int): BigInteger

  /**
   * Read an 8-bit two's-compliment integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for an 8-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readInt8(): Int = readInt(8)

  /**
   * Read a 16-bit two's-compliment integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 16-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readInt16(): Int = readInt(16)

  /**
   * Read a 32-bit two's-compliment integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 32-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readInt32(): Int = readInt(32)

  /**
   * Read a 64-bit two's-compliment integer from the SSZ source.
   *
   * @return A long.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 64-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readInt64(): Long = readLong(64)

  /**
   * Read an unsigned int value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return An unsigned int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length, or the decoded
   * value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt(bitLength: Int): UInt = readInt(bitLength).toUInt()

  /**
   * Read an unsigned long value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return An unsigned long.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length, or the decoded
   * value was too large to fit into a long.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readULong(bitLength: Int): ULong = readLong(bitLength).toULong()

  /**
   * Read an unsigned big integer value from the SSZ source.
   *
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return A big integer.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUnsignedBigInteger(bitLength: Int): BigInteger

  /**
   * Read an 8-bit unsigned integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for an 8-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt8(): UInt = readUInt(8)

  /**
   * Read a 16-bit unsigned integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 16-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt16(): UInt = readUInt(16)

  /**
   * Read a 32-bit unsigned integer from the SSZ source.
   *
   * @return An int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 32-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt32(): ULong = readULong(32)

  /**
   * Read a 64-bit unsigned integer from the SSZ source.
   *
   * @return A long.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 64-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt64(): ULong = readULong(64)

  /**
   * Read a [UInt256] from the SSZ source.
   *
   * @return A [UInt256].
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 256-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt256(): UInt256

  /**
   * Read a [UInt384] from the SSZ source.
   *
   * @return A [UInt384].
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 384-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt384(): UInt384

  /**
   * Read a boolean from the SSZ source.
   *
   * @return A boolean.
   * @throws InvalidSSZTypeException If the decoded value is not a boolean.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readBoolean(): Boolean = when (readInt(8)) {
    0 -> false
    1 -> true
    else -> throw InvalidSSZTypeException("decoded value is not a boolean")
  }

  /**
   * Read a 20-byte address from the SSZ source.
   *
   * @return The bytes of the Address.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 20-byte address.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readAddress(): Bytes

  /**
   * Read a hash from the SSZ source.
   *
   * @param hashLength The length of the hash (in bytes).
   * @return The bytes of the hash.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for a 32-byte hash.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readHash(hashLength: Int): Bytes

  /**
   * Read a list of [Bytes] from the SSZ source.
   *
   * Note: prefer to use [.readBytesList] instead, especially when reading untrusted data.
   *
   * @return A list of [Bytes].
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   * any byte array is too large (greater than 2^32 bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readBytesList(): List<Bytes> = readBytesList(Integer.MAX_VALUE)

  /**
   * Read a list of [Bytes] from the SSZ source.
   *
   * @param limit The maximum number of bytes to read for each list element.
   * @return A list of [Bytes].
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   * the size of any byte array would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readBytesList(limit: Int): List<Bytes>

  /**
   * Read a list of byte arrays from the SSZ source.
   *
   * Note: prefer to use [.readByteArrayList] instead, especially when reading untrusted data.
   *
   * @return A list of byte arrays.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   * any byte array is too large (greater than 2^32 bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readByteArrayList(): List<ByteArray> = readByteArrayList(Integer.MAX_VALUE)

  /**
   * Read a list of byte arrays from the SSZ source.
   *
   * @param limit The maximum number of bytes to read for each list element.
   * @return A list of byte arrays.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a byte array, or
   * the size of any byte array would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readByteArrayList(limit: Int): List<ByteArray>

  /**
   * Read a list of strings from the SSZ source.
   *
   * Note: prefer to use [.readStringList] instead, especially when reading untrusted data.
   *
   * @return A list of strings.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a string, or any
   * string is too large (greater than 2^32 bytes).
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readStringList(): List<String> = readStringList(Integer.MAX_VALUE)

  /**
   * Read a list of strings from the SSZ source.
   *
   * @param limit The maximum number of bytes to read for each list element.
   * @return A list of strings.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, any value in the list is not a string, or the
   * size of any string would exceed the limit.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readStringList(limit: Int): List<String>

  /**
   * Read a list of two's-compliment int values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readIntList(bitLength: Int): List<Int>

  /**
   * Read a list of two's-compliment long int values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of longs.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into a long.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readLongIntList(bitLength: Int): List<Long>

  /**
   * Read a list of two's-compliment big integer values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of big integers.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, or there are insufficient encoded bytes for
   * the desired bit length or any value in the list.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readBigIntegerList(bitLength: Int): List<BigInteger>

  /**
   * Read a list of 8-bit two's-compliment int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readInt8List(): List<Int> = readIntList(8)

  /**
   * Read a list of 16-bit two's-compliment int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readInt16List(): List<Int> = readIntList(16)

  /**
   * Read a list of 32-bit two's-compliment int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readInt32List(): List<Int> = readIntList(32)

  /**
   * Read a list of 64-bit two's-compliment int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readInt64List(): List<Long> = readLongIntList(64)

  /**
   * Read a list of unsigned int values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUIntList(bitLength: Int): List<UInt> {
    // encoding is the same for unsigned
    return readIntList(bitLength).map { i -> i.toUInt() }
  }

  /**
   * Read a list of unsigned long int values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of longs.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into a long.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readULongIntList(bitLength: Int): List<ULong> {
    // encoding is the same for unsigned
    return readLongIntList(bitLength).map { i -> i.toULong() }
  }

  /**
   * Read a list of unsigned big integer values from the SSZ source.
   *
   * @param bitLength The bit length of the integers to read (a multiple of 8).
   * @return A list of big integers.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, or there are insufficient encoded bytes for
   * the desired bit length or any value in the list.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUnsignedBigIntegerList(bitLength: Int): List<BigInteger>

  /**
   * Read a list of 8-bit unsigned int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt8List(): List<UInt> = readUIntList(8)

  /**
   * Read a list of 16-bit unsigned int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt16List(): List<UInt> = readUIntList(16)

  /**
   * Read a list of 32-bit unsigned int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt32List(): List<ULong> = readULongIntList(32)

  /**
   * Read a list of 64-bit unsigned int values from the SSZ source.
   *
   * @return A list of ints.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt64List(): List<ULong> = readULongIntList(64)

  /**
   * Read a list of 256-bit unsigned int values from the SSZ source.
   *
   * @return A list of [UInt256].
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt256List(): List<UInt256>

  /**
   * Read a list of 384-bit unsigned int values from the SSZ source.
   *
   * @return A list of [UInt256].
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for the
   * desired bit length or any value in the list, or any decoded value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readUInt384List(): List<UInt384>

  /**
   * Read a list of 20-byte addresses from the SSZ source.
   *
   * @return A list of 20-byte addresses.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for any
   * address in the list.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readAddressList(): List<Bytes>

  /**
   * Read a list of hashes from the SSZ source.
   *
   * @param hashLength The length of the hash (in bytes).
   * @return A list of 32-byte hashes.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for any
   * hash in the list.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readHashList(hashLength: Int): List<Bytes>

  /**
   * Read a list of booleans from the SSZ source.
   *
   * @return A list of booleans.
   * @throws InvalidSSZTypeException If the next SSZ value is not a list, there are insufficient encoded bytes for all
   * the booleans in the list.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun readBooleanList(): List<Boolean>

  /**
   * Check if all values have been read.
   *
   * @return `true` if all values have been read.
   */
  val isComplete: Boolean
}
