// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.eth

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.MemoryTransactionPool
import org.apache.tuweni.genesis.Genesis
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.rlpx.wire.WireConnection
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(BouncyCastleExtension::class)
class EthControllerTest {

  @Test
  fun testMismatchHeaders() = runBlocking {
    val requestsManager = EthClient66(mock {}, mock {}, mock {})
    val controller = EthController(
      BlockchainRepository.inMemory(Genesis.dev()),
      MemoryTransactionPool(),
      requestsManager,
    ) { _, _ -> }
    val conn = mock<WireConnection> {}

    controller.addNewBlockBodies(conn, Bytes.fromHexString("0x0010"), listOf(BlockBody(emptyList(), emptyList())))
  }
}
