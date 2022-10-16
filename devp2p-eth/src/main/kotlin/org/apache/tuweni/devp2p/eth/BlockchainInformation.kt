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

/**
 * Blockchain information to be shared over the network with peers
 */
interface BlockchainInformation {
  /**
   * Our network ID. 1 for mainnet.
   * @return our network ID
   */
  fun networkID(): UInt256

  /**
   * Total difficulty of our canonical chain
   * @return our best block total difficulty
   */
  fun totalDifficulty(): UInt256

  /**
   * Best block hash we know
   * @return our best block hash from our chain
   */
  fun bestHash(): Hash

  /**
   * Best block number we know
   *
   * @return our best block number from our chain
   */
  fun bestNumber(): UInt256

  /**
   * Genesis block hash
   *
   * @return the genesis block hash
   */
  fun genesisHash(): Hash

  /**
   * Forks of our network we know
   *
   * @param all fork block numbers
   */
  fun forks(): List<Long>

  /**
   * Get all our fork hashes
   *
   * @param all fork hashes, sorted
   */
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

  fun getLastestApplicableFork(number: Long): ForkInfo
}

data class ForkInfo(val next: Long, val hash: Bytes)

/**
 * POJO - constant representation of the blockchain information
 *
 * @param networkID the network ID
 * @param totalDifficulty the total difficulty of the chain
 * @param bestHash best known hash
 * @param bestNumber best known number
 * @param genesisHash the genesis block hash
 * @param forks known forks
 */
class SimpleBlockchainInformation(
  val networkID: UInt256,
  val totalDifficulty: UInt256,
  val bestHash: Hash,
  val bestNumber: UInt256,
  val genesisHash: Hash,
  possibleForks: List<Long>
) : BlockchainInformation {

  private val forkIds: List<ForkInfo>
  private val forks: List<Long>
  private val genesisHashCrc: Bytes

  init {
    this.forks = possibleForks.filter { it > 0 }.sorted().distinct()
    val crc = CRC32()
    crc.update(genesisHash.toArrayUnsafe())
    genesisHashCrc = Bytes.ofUnsignedInt(crc.value)
    val forkHashes = mutableListOf(genesisHashCrc)
    for (f in forks) {
      val byteRepresentationFork = Bytes.ofUnsignedLong(f).toArrayUnsafe()
      crc.update(byteRepresentationFork, 0, byteRepresentationFork.size)
      forkHashes.add(Bytes.ofUnsignedInt(crc.value))
    }
    val mutableForkIds = mutableListOf<ForkInfo>()

    // This loop is for all the fork hashes that have an associated "next fork"
    for (i in forks.indices) {
      mutableForkIds.add(ForkInfo(forks.get(i), forkHashes[i]))
    }
    if (forks.isNotEmpty()) {
      mutableForkIds.add(ForkInfo(0, forkHashes.last()))
    }
    this.forkIds = mutableForkIds.toList()
  }

  override fun networkID(): UInt256 = networkID

  override fun totalDifficulty(): UInt256 = totalDifficulty

  override fun bestHash(): Hash = bestHash

  override fun bestNumber(): UInt256 = bestNumber

  override fun genesisHash(): Hash = genesisHash

  override fun forks(): List<Long> = forks

  override fun getLastestApplicableFork(number: Long): ForkInfo {
    for (fork in forkIds) {
      if (number < fork.next) {
        return fork
      }
    }
    return forkIds.lastOrNull() ?: ForkInfo(0, genesisHashCrc)
  }
}
