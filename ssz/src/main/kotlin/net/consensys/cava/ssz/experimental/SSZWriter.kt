/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.ssz.experimental

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.ssz.SSZ
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt384
import java.math.BigInteger

@ExperimentalUnsignedTypes
interface SSZWriter {

  /**
   * Append an already SSZ encoded value.
   *
   * Note that this method **may not** validate that `value` is a valid SSZ sequence. Appending an invalid SSZ
   * sequence will cause the entire SSZ encoding produced by this writer to also be invalid.
   *
   * @param value the SSZ encoded bytes to append
   */
  fun writeSSZ(value: Bytes)

  /**
   * Append an already SSZ encoded value.
   *
   * Note that this method **may not** validate that `value` is a valid SSZ sequence. Appending an invalid SSZ
   * sequence will cause the entire SSZ encoding produced by this writer to also be invalid.
   *
   * @param value the SSZ encoded bytes to append
   */
  fun writeSSZ(value: ByteArray) = writeSSZ(Bytes.wrap(value))

  /**
   * Encode a [Bytes] value to SSZ.
   *
   * @param value the byte array to encode
   */
  fun writeBytes(value: Bytes)

  /**
   * Encode a byte array to SSZ.
   *
   * @param value the byte array to encode
   */
  fun writeBytes(value: ByteArray)

  /**
   * Write a string to the output.
   *
   * @param str the string to write
   */
  fun writeString(str: String)

  /**
   * Write a two's-compliment integer to the output.
   *
   * @param value the integer to write
   * @param bitLength the bit length of the integer value
   * @throws IllegalArgumentException if the value is too large for the specified bit length
   */
  fun writeInt(value: Int, bitLength: Int)

  /**
   * Write a two's-compliment long to the output.
   *
   * @param value the long value to write
   * @param bitLength the bit length of the integer value
   * @throws IllegalArgumentException if the value is too large for the specified bit length
   */
  fun writeLong(value: Long, bitLength: Int)

  /**
   * Write a big integer to the output.
   *
   * @param value the integer to write
   * @param bitLength the bit length of the integer value
   * @throws IllegalArgumentException if the value is too large for the specified bit length
   */
  fun writeBigInteger(value: BigInteger, bitLength: Int) {
    writeSSZ(SSZ.encodeBigIntegerToByteArray(value, bitLength))
  }

  /**
   * Write an 8-bit two's-compliment integer to the output.
   *
   * @param value the integer to write
   * @throws IllegalArgumentException if the value is too large to be represented in 8 bits
   */
  fun writeInt8(value: Int) {
    writeInt(value, 8)
  }

  /**
   * Write a 16-bit two's-compliment integer to the output.
   *
   * @param value the integer to write
   * @throws IllegalArgumentException if the value is too large to be represented in 16 bits
   */
  fun writeInt16(value: Int) {
    writeInt(value, 16)
  }

  /**
   * Write a 32-bit two's-compliment integer to the output.
   *
   * @param value the integer to write
   */
  fun writeInt32(value: Int) {
    writeInt(value, 32)
  }

  /**
   * Write a 64-bit two's-compliment integer to the output.
   *
   * @param value the long to write
   */
  fun writeInt64(value: Long) {
    writeLong(value, 64)
  }

  /**
   * Write an unsigned integer to the output.
   *
   * @param value the integer to write
   * @param bitLength the bit length of the integer value
   * @throws IllegalArgumentException if the value is too large for the specified bit length
   */
  fun writeUInt(value: UInt, bitLength: Int)

  /**
   * Write an unsigned long to the output.
   *
   * @param value the long value to write
   * @param bitLength the bit length of the integer value
   * @throws IllegalArgumentException if the value is too large for the specified bit length
   */
  fun writeULong(value: ULong, bitLength: Int)

  /**
   * Write an 8-bit unsigned integer to the output.
   *
   * @param value the integer to write
   * @throws IllegalArgumentException if the value is too large to be represented in 8 bits
   */
  fun writeUInt8(value: UInt) {
    writeUInt(value, 8)
  }

  /**
   * Write a 16-bit unsigned integer to the output.
   *
   * @param value the integer to write
   * @throws IllegalArgumentException if the value is too large to be represented in 16 bits
   */
  fun writeUInt16(value: UInt) {
    writeUInt(value, 16)
  }

  /**
   * Write a 32-bit unsigned integer to the output.
   *
   * @param value the integer to write
   */
  fun writeUInt32(value: UInt) {
    writeUInt(value, 32)
  }

  /**
   * Write a 64-bit unsigned integer to the output.
   *
   * @param value the long to write
   */
  fun writeUInt64(value: ULong) {
    writeULong(value, 64)
  }

  /**
   * Write a [UInt256] to the output.
   *
   * @param value the [UInt256] to write
   */
  fun writeUInt256(value: UInt256) {
    writeSSZ(SSZ.encodeUInt256(value))
  }

  /**
   * Write a boolean to the output.
   *
   * @param value the boolean value
   */
  fun writeBoolean(value: Boolean) {
    writeSSZ(SSZ.encodeBoolean(value))
  }

  /**
   * Write an address.
   *
   * @param address the address (must be exactly 20 bytes)
   * @throws IllegalArgumentException if `address.size != 20`
   */
  fun writeAddress(address: Bytes) {
    writeSSZ(SSZ.encodeAddress(address))
  }

  /**
   * Write a hash.
   *
   * @param hash the hash
   */
  fun writeHash(hash: Bytes) {
    writeSSZ(SSZ.encodeHash(hash))
  }

  /**
   * Write a list of bytes.
   *
   * @param elements the bytes to write as a list
   */
  fun writeBytesList(vararg elements: Bytes)

  /**
   * Write a list of bytes.
   *
   * @param elements the bytes to write as a list
   */
  fun writeBytesList(elements: List<Bytes>)

  /**
   * Write a list of strings, which must be of the same length
   *
   * @param elements the strings to write as a list
   */
  fun writeStringList(vararg elements: String)

  /**
   * Write a list of strings, which must be of the same length
   *
   * @param elements the strings to write as a list
   */
  fun writeStringList(elements: List<String>)

  /**
   * Write a list of two's compliment integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  fun writeIntList(bitLength: Int, vararg elements: Int)

  /**
   * Write a list of two's compliment integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  fun writeIntList(bitLength: Int, elements: List<Int>)

  /**
   * Write a list of two's compliment long integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the long integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  fun writeLongIntList(bitLength: Int, vararg elements: Long)

  /**
   * Write a list of two's compliment long integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the long integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  fun writeLongIntList(bitLength: Int, elements: List<Long>)

  /**
   * Write a list of big integers.
   *
   * @param bitLength the bit length of each integer
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if an integer cannot be stored in the number of bytes provided
   */
  fun writeBigIntegerList(bitLength: Int, vararg elements: BigInteger)

  /**
   * Write a list of big integers.
   *
   * @param bitLength the bit length of each integer
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if an integer cannot be stored in the number of bytes provided
   */
  fun writeBigIntegerList(bitLength: Int, elements: List<BigInteger>)

  /**
   * Write a list of 8-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  fun writeInt8List(vararg elements: Int) {
    writeIntList(8, *elements)
  }

  /**
   * Write a list of 8-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  fun writeInt8List(elements: List<Int>) {
    writeIntList(8, elements)
  }

  /**
   * Write a list of 16-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  fun writeInt16List(vararg elements: Int) {
    writeIntList(16, *elements)
  }

  /**
   * Write a list of 16-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  fun writeInt16List(elements: List<Int>) {
    writeIntList(16, elements)
  }

  /**
   * Write a list of 32-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeInt32List(vararg elements: Int) {
    writeIntList(32, *elements)
  }

  /**
   * Write a list of 32-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeInt32List(elements: List<Int>) {
    writeIntList(32, elements)
  }

  /**
   * Write a list of 64-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeInt64List(vararg elements: Long) {
    writeLongIntList(64, *elements)
  }

  /**
   * Write a list of 64-bit two's compliment integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeInt64List(elements: List<Long>) {
    writeLongIntList(64, elements)
  }

  /**
   * Write a list of unsigned integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  fun writeUIntList(bitLength: Int, vararg elements: UInt)

  /**
   * Write a list of unsigned integers.
   *
   * @param bitLength the bit length of the encoded integers (must be a multiple of 8)
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large for the specified bit length
   */
  fun writeUIntList(bitLength: Int, elements: List<UInt>)

  /**
   * Write a list of unsigned long integers.
   *
   * @param bitLength The bit length of the encoded integers (must be a multiple of 8).
   * @param elements The long integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeULongIntList(bitLength: Int, vararg elements: ULong)

  /**
   * Write a list of unsigned long integers.
   *
   * @param bitLength The bit length of the encoded integers (must be a multiple of 8).
   * @param elements The long integers to write as a list.
   * @throws IllegalArgumentException If any values are too large for the specified bit length.
   */
  fun writeULongIntList(bitLength: Int, elements: List<ULong>)

  /**
   * Write a list of 8-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  fun writeUInt8List(vararg elements: UInt) {
    writeUIntList(8, *elements)
  }

  /**
   * Write a list of 8-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 8 bits
   */
  fun writeUInt8List(elements: List<UInt>) {
    writeUIntList(8, elements)
  }

  /**
   * Write a list of 16-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  fun writeUInt16List(vararg elements: UInt) {
    writeUIntList(16, *elements)
  }

  /**
   * Write a list of 16-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   * @throws IllegalArgumentException if any values are too large to be represented in 16 bits
   */
  fun writeUInt16List(elements: List<UInt>) {
    writeUIntList(16, elements)
  }

  /**
   * Write a list of 32-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeUInt32List(vararg elements: UInt) {
    writeUIntList(32, *elements)
  }

  /**
   * Write a list of 32-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeUInt32List(elements: List<UInt>) {
    writeUIntList(32, elements)
  }

  /**
   * Write a list of 64-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeUInt64List(vararg elements: ULong) {
    writeULongIntList(64, *elements)
  }

  /**
   * Write a list of 64-bit unsigned integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeUInt64List(elements: List<ULong>) {
    writeULongIntList(64, elements)
  }

  /**
   * Write a list of unsigned 256-bit integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeUInt256List(vararg elements: UInt256)

  /**
   * Write a list of unsigned 256-bit integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeUInt256List(elements: List<UInt256>)

  /**
   * Write a list of unsigned 384-bit integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeUInt384List(vararg elements: UInt384)

  /**
   * Write a list of unsigned 256-bit integers.
   *
   * @param elements the integers to write as a list
   */
  fun writeUInt384List(elements: List<UInt384>)

  /**
   * Write a list of hashes.
   *
   * @param elements the hashes to write as a list
   */
  fun writeHashList(vararg elements: Bytes)

  /**
   * Write a list of hashes.
   *
   * @param elements the hashes to write as a list
   */
  fun writeHashList(elements: List<Bytes>)

  /**
   * Write a list of addresses.
   *
   * @param elements the addresses to write as a list
   * @throws IllegalArgumentException if any `address.size != 20`
   */
  fun writeAddressList(vararg elements: Bytes)

  /**
   * Write a list of addresses.
   *
   * @param elements the addresses to write as a list
   * @throws IllegalArgumentException if any `address.size != 20`
   */
  fun writeAddressList(elements: List<Bytes>)

  /**
   * Write a list of booleans.
   *
   * @param elements the booleans to write as a list
   */
  fun writeBooleanList(vararg elements: Boolean)

  /**
   * Write a list of booleans.
   *
   * @param elements the booleans to write as a list
   */
  fun writeBooleanList(elements: List<Boolean>)
}
