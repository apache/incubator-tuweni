// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.les

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.devp2p.eth.RandomConnectionSelectionStrategy
import org.apache.tuweni.devp2p.eth.SimpleBlockchainInformation
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.MemoryTransactionPool
import org.apache.tuweni.genesis.Genesis
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.TempDirectoryExtension
import org.apache.tuweni.rlpx.MemoryWireConnectionsRepository
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.units.bigints.UInt256
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TempDirectoryExtension::class, BouncyCastleExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LESSubprotocolTest {

  private val blockchainInformation = SimpleBlockchainInformation(
    UInt256.valueOf(42L),
    UInt256.ONE,
    Hash.fromBytes(Bytes32.random()),
    UInt256.valueOf(42L),
    Hash.fromBytes(Bytes32.random()),
    emptyList()
  )

  @Test
  @Throws(Exception::class)
  fun supportsLESv2() = runBlocking {
    val sp = LESSubprotocol(
      Dispatchers.Default,
      blockchainInformation,
      true,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      BlockchainRepository.inMemory(Genesis.dev()),
      MemoryTransactionPool(),
      RandomConnectionSelectionStrategy(MemoryWireConnectionsRepository())
    )
    assertTrue(sp.supports(SubProtocolIdentifier.of("les", 2)))
  }

  @Test
  @Throws(Exception::class)
  fun noSupportForv3() = runBlocking {
    val sp = LESSubprotocol(
      Dispatchers.Default,
      blockchainInformation,
      true,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      BlockchainRepository.inMemory(Genesis.dev()),
      MemoryTransactionPool(),
      RandomConnectionSelectionStrategy(MemoryWireConnectionsRepository())
    )
    assertFalse(sp.supports(SubProtocolIdentifier.of("les", 3)))
  }

  @Test
  @Throws(Exception::class)
  fun noSupportForETH() = runBlocking {
    val sp = LESSubprotocol(
      Dispatchers.Default,
      blockchainInformation,
      true,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      BlockchainRepository.inMemory(Genesis.dev()),
      MemoryTransactionPool(),
      RandomConnectionSelectionStrategy(MemoryWireConnectionsRepository())
    )
    assertFalse(sp.supports(SubProtocolIdentifier.of("eth", 2)))
  }
}
