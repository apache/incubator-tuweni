// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.blockprocessor

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.precompiles.Registry
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.evm.HardFork
import org.apache.tuweni.genesis.Genesis
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64
import org.apache.tuweni.units.ethereum.Gas
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(BouncyCastleExtension::class)
class BlockProcessorTest {

  /**
   * Demonstrate how the proto block can be created.
   */
  @Test
  fun testValidBlockNoTransactions() = runBlocking {
    val processor = BlockProcessor(UInt256.ONE)
    val repository = BlockchainRepository.inMemory(Genesis.dev())
    val result = processor.execute(
      Genesis.dev().header,
      Address.ZERO,
      Gas.valueOf(100),
      Gas.ZERO,
      UInt256.ZERO,
      listOf(),
      repository,
      Registry.istanbul,
      HardFork.HOMESTEAD
    )
    val block = result.block.toBlock(
      listOf(),
      Address.ZERO,
      UInt256.ONE,
      Instant.now(),
      Bytes.EMPTY,
      Genesis.emptyHash,
      UInt64.random()
    )
    assertEquals(0, block.body.transactions.size)
  }
}
