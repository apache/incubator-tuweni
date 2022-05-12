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
package org.apache.tuweni.evm.impl

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.MutableBytes
import org.apache.tuweni.units.bigints.UInt256
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer

class Memory {

  companion object {
    val capacity = 1000000000
    val logger = LoggerFactory.getLogger(Memory::class.java)
  }

  var wordsSize = UInt256.ZERO
  var memoryData: MutableBytes? = null

  fun write(offset: UInt256, sourceOffset: UInt256, numBytes: UInt256, code: Bytes): Boolean {
    logger.trace("Write to memory at offset $offset, size $numBytes")
    val maxDistance = offset.add(numBytes)
    if (!offset.fitsInt() || !numBytes.fitsInt() || !maxDistance.fitsInt() || maxDistance.intValue() > capacity) {
      logger.warn("Memory write aborted, values too large")
      return false
    }
    var localMemoryData = memoryData
    if (localMemoryData == null) {
      localMemoryData = MutableBytes.wrapByteBuffer(ByteBuffer.allocate(maxDistance.intValue()))
    } else if (localMemoryData.size() < maxDistance.intValue()) {
      val buffer = ByteBuffer.allocate(maxDistance.intValue() * 2)
      buffer.put(localMemoryData.toArrayUnsafe())
      localMemoryData = MutableBytes.wrapByteBuffer(buffer)
    }
    memoryData = localMemoryData
    if (sourceOffset.fitsInt() && sourceOffset.intValue() < code.size()) {
      val maxCodeLength = code.size() - sourceOffset.intValue()
      val length = if (maxCodeLength < numBytes.intValue()) maxCodeLength else numBytes.intValue()
      val toWrite = code.slice(sourceOffset.intValue(), length)
      logger.trace("Writing $toWrite")
      memoryData!!.set(offset.intValue(), toWrite)
    }

    wordsSize = newSize(offset, numBytes)
    return true
  }

  fun allocatedBytes(): UInt256 {
    return wordsSize.multiply(32)
  }

  fun size(): UInt256 {
    return wordsSize
  }

  fun newSize(memOffset: UInt256, length: UInt256): UInt256 {
    if (length.isZero) {
      return wordsSize
    }

    val candidate = memOffset.add(length)
    if (candidate < memOffset || candidate < length) { // overflow
      return UInt256.MAX_VALUE
    }
    val candidateWords = candidate.divideCeil(32)
    return if (wordsSize > candidateWords) wordsSize else candidateWords
  }

  fun read(from: UInt256, length: UInt256, updateMemorySize: Boolean = true): Bytes? {
    if (length.isZero) {
      return Bytes.EMPTY
    }
    val max = from.add(length)
    if (!from.fitsInt() || !length.fitsInt() || !max.fitsInt()) {
      return null
    }
    if (updateMemorySize) {
      wordsSize = newSize(from, length)
    }

    val localMemoryData = memoryData
    if (localMemoryData != null) {
      if (from.intValue() >= localMemoryData.size()) {
        return Bytes.repeat(0, length.intValue())
      }
      if (localMemoryData.size() < max.intValue()) {
        val l = max.intValue() - localMemoryData.size()
        return Bytes.concatenate(
          localMemoryData.slice(from.intValue(), length.intValue() - l),
          Bytes.repeat(0, l)
        )
      }
      return localMemoryData.slice(from.intValue(), length.intValue())
    } else {
      return Bytes.repeat(0, length.intValue())
    }
  }
}
