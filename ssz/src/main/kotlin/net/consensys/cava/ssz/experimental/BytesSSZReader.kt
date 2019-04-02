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
import net.consensys.cava.units.bigints.UInt256
import net.consensys.cava.units.bigints.UInt384
import java.math.BigInteger

@ExperimentalUnsignedTypes
internal class BytesSSZReader(private val delegate: net.consensys.cava.ssz.SSZReader) : SSZReader {
  override fun readBytes(limit: Int): Bytes =
    delegate.readBytes(limit)

  override fun readInt(bitLength: Int): Int =
    delegate.readInt(bitLength)

  override fun readLong(bitLength: Int): Long =
    delegate.readLong(bitLength)

  override fun readBigInteger(bitLength: Int): BigInteger =
    delegate.readBigInteger(bitLength)

  override fun readUnsignedBigInteger(bitLength: Int): BigInteger =
    delegate.readUnsignedBigInteger(bitLength)

  override fun readUInt256(): UInt256 =
    delegate.readUInt256()

  override fun readUInt384(): UInt384 =
    delegate.readUInt384()

  override fun readAddress(): Bytes =
    delegate.readAddress()

  override fun readHash(hashLength: Int): Bytes =
    delegate.readHash(hashLength)

  override fun readBytesList(limit: Int): List<Bytes> =
    delegate.readBytesList(limit)

  override fun readByteArrayList(limit: Int): List<ByteArray> =
    delegate.readByteArrayList(limit)

  override fun readStringList(limit: Int): List<String> =
    delegate.readStringList(limit)

  override fun readIntList(bitLength: Int): List<Int> =
    delegate.readIntList(bitLength)

  override fun readLongIntList(bitLength: Int): List<Long> =
    delegate.readLongIntList(bitLength)

  override fun readBigIntegerList(bitLength: Int): List<BigInteger> =
    delegate.readBigIntegerList(bitLength)

  override fun readUnsignedBigIntegerList(bitLength: Int): List<BigInteger> =
    delegate.readUnsignedBigIntegerList(bitLength)

  override fun readUInt256List(): List<UInt256> =
    delegate.readUInt256List()

  override fun readUInt384List(): List<UInt384> =
    delegate.readUInt384List()

  override fun readAddressList(): List<Bytes> =
    delegate.readAddressList()

  override fun readHashList(hashLength: Int): List<Bytes> =
    delegate.readHashList(hashLength)

  override fun readBooleanList(): List<Boolean> =
    delegate.readBooleanList()

  override val isComplete: Boolean
    get() = delegate.isComplete
}
