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
   * Get our latest known fork
   *
   * @return the latest fork number we know
   */
  fun getLatestFork(): Long? {
    return if (forks().isEmpty()) null else forks()[forks().size - 1]
  }

  /**
   * Get all our fork hashes
   *
   * @param all fork hashes, sorted
   */
  fun getForkHashes(): List<Bytes> {
    val crc = CRC32()
    crc.update(genesisHash().toArrayUnsafe())
    val forkHashes = ArrayList<Bytes>(listOf(Bytes.ofUnsignedInt(crc.value)))
    val forks = mutableListOf<Long>()
    forks.addAll(forks())
    for (fork in forks) {
      val byteRepresentationFork = UInt64.valueOf(fork).toBytes()
      crc.update(byteRepresentationFork.toArrayUnsafe(), 0, byteRepresentationFork.size())
      forkHashes.add(Bytes.ofUnsignedInt(crc.value))
    }
    return forkHashes
  }

  /**
   * Get latest fork hash, if known
   *
   * @return the hash of the latest fork hash we know of
   */
  fun getLatestForkHash(): Bytes? {
    val hashes = getForkHashes()
    return if (hashes.isEmpty()) {
      null
    } else hashes[hashes.size - 1]
  }
}

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
data class SimpleBlockchainInformation(
  val networkID: UInt256,
  val totalDifficulty: UInt256,
  val bestHash: Hash,
  val bestNumber: UInt256,
  val genesisHash: Hash,
  val forks: List<Long>
) : BlockchainInformation {

  override fun networkID(): UInt256 = networkID

  override fun totalDifficulty(): UInt256 = totalDifficulty

  override fun bestHash(): Hash = bestHash

  override fun bestNumber(): UInt256 = bestNumber

  override fun genesisHash(): Hash = genesisHash

  override fun forks(): List<Long> = forks
}
