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
package org.apache.tuweni.devp2p.eth

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64
import java.util.zip.CRC32

interface BlockchainInformation {
  fun networkID(): UInt256
  fun totalDifficulty(): UInt256
  fun bestHash(): Hash
  fun genesisHash(): Hash
  fun forks(): List<Long>

  fun getLatestFork(): Long? {
    return if (forks().isEmpty()) null else forks()[forks().size - 1]
  }

  fun getForkHashes(): List<Bytes> {
    val crc = CRC32()
    crc.update(genesisHash().toArrayUnsafe())
    val forkHashes = ArrayList<Bytes>(listOf(Bytes.ofUnsignedInt(crc.value)))
    for (fork in forks()) {
      val byteRepresentationFork = UInt64.valueOf(fork).toBytes()
      crc.update(byteRepresentationFork.toArrayUnsafe(), 0, byteRepresentationFork.size())
      forkHashes.add(Bytes.ofUnsignedInt(crc.value))
    }
    return forkHashes
  }

  fun getLatestForkHash(): Bytes? {
    val hashes = getForkHashes()
    return if (hashes.isEmpty()) {
      null
    } else hashes[hashes.size - 1]
  }
}

data class SimpleBlockchainInformation(
  val networkID: UInt256,
  val totalDifficulty: UInt256,
  val bestHash: Hash,
  val genesisHash: Hash,
  val forks: List<Long>
) : BlockchainInformation {

  private val forksWithUpperBound: MutableList<Long> = mutableListOf()
  init {
    forksWithUpperBound.addAll(forks)
    forksWithUpperBound.add(0L)
  }

  override fun networkID(): UInt256 = networkID

  override fun totalDifficulty(): UInt256 = totalDifficulty

  override fun bestHash(): Hash = bestHash

  override fun genesisHash(): Hash = genesisHash

  override fun forks(): List<Long> = forksWithUpperBound
}
