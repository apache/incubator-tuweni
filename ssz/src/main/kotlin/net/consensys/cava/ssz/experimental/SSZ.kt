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
import java.nio.BufferOverflowException
import java.nio.ByteBuffer
import java.nio.ReadOnlyBufferException

/**
 * Simple Serialize (SSZ) encoding and decoding.
 */
@ExperimentalUnsignedTypes
object SSZ {

  // Encoding

  /**
   * Encode values to a {@link Bytes} value.
   *
   * @param fn A consumer that will be provided with a {@link SSZWriter} that can consume values.
   * @return The SSZ encoding in a {@link Bytes} value.
   */
  fun encode(fn: (SSZWriter) -> Unit): Bytes = net.consensys.cava.ssz.SSZ.encode { w -> fn(BytesSSZWriter(w)) }

  /**
   * Encode values to a {@link ByteBuffer}.
   *
   * @param buffer The buffer to write into, starting from its current position.
   * @param fn A consumer that will be provided with a {@link SSZWriter} that can consume values.
   * @param <T> The type of the buffer.
   * @return The buffer.
   * @throws BufferOverflowException If the writer attempts to write more than the provided buffer can hold.
   * @throws ReadOnlyBufferException If the provided buffer is read-only.
   */
  fun <T : ByteBuffer> encodeTo(buffer: T, fn: (SSZWriter) -> Unit): T =
    net.consensys.cava.ssz.SSZ.encodeTo(buffer) { w -> fn(BytesSSZWriter(w)) }

  /**
   * Encode an unsigned integer to a {@link Bytes} value.
   *
   * @param value The integer to encode.
   * @param bitLength The bit length of the encoded integer value (must be a multiple of 8).
   * @return The SSZ encoding in a {@link Bytes} value.
   * @throws IllegalArgumentException If the value is too large for the specified {@code bitLength}.
   */
  fun encodeUInt(value: UInt, bitLength: Int): Bytes =
    net.consensys.cava.ssz.SSZ.encodeULong(value.toLong(), bitLength)

  /**
   * Encode an unsigned long integer to a {@link Bytes} value.
   *
   * @param value The long to encode.
   * @param bitLength The bit length of the integer value (must be a multiple of 8).
   * @return The SSZ encoding in a {@link Bytes} value.
   * @throws IllegalArgumentException If the value is too large for the specified {@code bitLength}.
   */
  fun encodeULong(value: ULong, bitLength: Int): Bytes =
    net.consensys.cava.ssz.SSZ.encodeULong(value.toLong(), bitLength)

  /**
   * Encode an 8-bit unsigned integer to a {@link Bytes} value.
   *
   * @param value The integer to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   * @throws IllegalArgumentException If the value is too large for the specified {@code bitLength}.
   */
  fun encodeUInt8(value: UInt): Bytes = encodeUInt(value, 8)

  /**
   * Encode a 16-bit unsigned integer to a {@link Bytes} value.
   *
   * @param value The integer to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   * @throws IllegalArgumentException If the value is too large for the specified {@code bitLength}.
   */
  fun encodeUInt16(value: UInt): Bytes = encodeUInt(value, 16)

  /**
   * Encode a 32-bit unsigned integer to a {@link Bytes} value.
   *
   * @param value The integer to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   * @throws IllegalArgumentException If the value is too large for the specified {@code bitLength}.
   */
  fun encodeUInt32(value: UInt): Bytes = encodeUInt(value, 32)

  /**
   * Encode a 64-bit unsigned integer to a {@link Bytes} value.
   *
   * @param value The integer to encode.
   * @return The SSZ encoding in a {@link Bytes} value.
   * @throws IllegalArgumentException If the value is too large for the specified {@code bitLength}.
   */
  fun encodeUInt64(value: ULong): Bytes = encodeULong(value, 64)

  // Decoding

  /**
   * Read and decode SSZ from a {@link Bytes} value.
   *
   * @param source The SSZ encoded bytes.
   * @param fn A function that will be provided a {@link SSZReader}.
   * @param <T> The result type of the reading function.
   * @return The result from the reading function.
   */
  fun <T> decode(source: Bytes, fn: (SSZReader) -> T): T =
    net.consensys.cava.ssz.SSZ.decode(source) { r -> fn(BytesSSZReader(r)) }

  /**
   * Read a SSZ encoded unsigned integer from a {@link Bytes} value.
   *
   * @param source The SSZ encoded bytes.
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return An unsigned int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length, or the decoded
   *         value was too large to fit into an int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun decodeUInt(source: Bytes, bitLength: Int): UInt =
    net.consensys.cava.ssz.SSZ.decodeUInt(source, bitLength).toUInt()

  /**
   * Read a SSZ encoded unsigned long integer from a {@link Bytes} value.
   *
   * @param source The SSZ encoded bytes.
   * @param bitLength The bit length of the integer to read (a multiple of 8).
   * @return An unsigned long.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for the desired bit length, or the decoded
   *         value was too large to fit into a long.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun decodeULong(source: Bytes, bitLength: Int): ULong =
    net.consensys.cava.ssz.SSZ.decodeULong(source, bitLength).toULong()

  /**
   * Read an 8-bit unsigned integer from the SSZ source.
   *
   * @param source The SSZ encoded bytes.
   * @return An unsigned int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for an 8-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun decodeUInt8(source: Bytes) = decodeUInt(source, 8)

  /**
   * Read a 16-bit unsigned integer from the SSZ source.
   *
   * @param source The SSZ encoded bytes.
   * @return An unsigned int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for an 8-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun decodeUInt16(source: Bytes) = decodeUInt(source, 16)

  /**
   * Read a 32-bit unsigned integer from the SSZ source.
   *
   * @param source The SSZ encoded bytes.
   * @return An unsigned int.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for an 8-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun decodeUInt32(source: Bytes) = decodeUInt(source, 32)

  /**
   * Read a 64-bit unsigned integer from the SSZ source.
   *
   * @param source The SSZ encoded bytes.
   * @return An unsigned long.
   * @throws InvalidSSZTypeException If there are insufficient encoded bytes for an 8-bit int.
   * @throws EndOfSSZException If there are no more SSZ values to read.
   */
  fun decodeUInt64(source: Bytes) = decodeULong(source, 64)
}
