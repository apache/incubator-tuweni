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

import kotlinx.coroutines.runBlocking
import org.apache.lucene.index.IndexWriter
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.eth.EthSubprotocol.Companion.ETH62
import org.apache.tuweni.devp2p.eth.EthSubprotocol.Companion.ETH64
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.LogsBloomFilter
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.MemoryTransactionPool
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.LuceneIndexWriter
import org.apache.tuweni.junit.LuceneIndexWriterExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.rlp.RLP
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.rlpx.wire.WireConnection
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(LuceneIndexWriterExtension::class, VertxExtension::class, BouncyCastleExtension::class)
class EthHandlerTest {

  companion object {

    private lateinit var genesisBlock: Block
    private lateinit var handler: EthHandler
    private lateinit var repository: BlockchainRepository
    private lateinit var service: RLPxService

    @BeforeAll
    @JvmStatic
    fun setup(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
      var header = BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        UInt256.ZERO,
        Gas.valueOf(3000),
        Gas.valueOf(2000),
        Instant.now().plusSeconds(30).truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4, 5, 6, 7, 8, 9, 10),
        Hash.fromBytes(Bytes32.random()),
        UInt64.ZERO
      )
      val body = BlockBody(
        listOf(
          Transaction(
            UInt256.valueOf(1),
            Wei.valueOf(2),
            Gas.valueOf(2),
            Address.fromBytes(Bytes.random(20)),
            Wei.valueOf(2),
            Bytes.random(12),
            SECP256K1.KeyPair.random()
          )
        ),
        listOf(header)
      )
      genesisBlock = Block(header, body)
      repository = BlockchainRepository.init(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer),
        genesisBlock
      )
      service = mock(RLPxService::class.java)
      val requestsManager = mock(EthRequestsManager::class.java)
      handler = EthHandler(
        blockchainInfo = SimpleBlockchainInformation(
          UInt256.valueOf(42L),
          UInt256.ONE,
          genesisBlock.header.hash,
          UInt256.valueOf(42L),
          genesisBlock.header.hash,
          emptyList()
        ),
        service = service,
        controller = EthController(repository, MemoryTransactionPool(), requestsManager)
      )

      for (i in 1..10) {
        val newBlock = createChildBlock(header)
        repository.storeBlock(newBlock)
        var txIndex = 0
        for (tx in newBlock.body.transactions) {
          repository.storeTransactionReceipt(
            TransactionReceipt(
              Bytes32.random(),
              32L,
              LogsBloomFilter(),
              emptyList()
            ),
            txIndex,
            tx.hash,
            newBlock.header.hash
          )
          txIndex++
        }
        header = newBlock.header
      }
    }

    private fun createChildBlock(parentBlock: BlockHeader): Block {
      val emptyListHash = Hash.hash(RLP.encodeList { })
      val emptyHash = Hash.hash(RLP.encode { writer -> writer.writeValue(Bytes.EMPTY) })
      val header = BlockHeader(
        parentBlock.hash,
        emptyListHash,
        Address.fromBytes(Bytes.random(20)),
        parentBlock.stateRoot,
        emptyHash,
        emptyHash,
        Bytes.wrap(ByteArray(256)),
        parentBlock.difficulty,
        parentBlock.number.add(1),
        parentBlock.gasLimit,
        Gas.valueOf(0),
        Instant.now(),
        Bytes.random(45),
        Hash.fromBytes(Bytes32.random()),
        UInt64.ZERO
      )
      val block = Block(
        header,
        BlockBody(
          listOf(
            Transaction(
              UInt256.ONE,
              Wei.valueOf(20L),
              Gas.valueOf(3000),
              Address.fromBytes(Bytes.random(20)),
              Wei.valueOf(1000),
              Bytes.EMPTY,
              SECP256K1.KeyPair.random()
            )
          ),
          emptyList()
        )
      )
      return block
    }
  }

  @Test
  fun testGetHeaders() = runBlocking {
    val conn = mock(WireConnection::class.java)
    `when`(conn.agreedSubprotocolVersion(eq(ETH62.name()))).thenReturn(ETH64)
    handler.handle(
      conn,
      MessageType.GetBlockHeaders.code,
      GetBlockHeaders(genesisBlock.header.hash, 3, 1, false).toBytes()
    ).await()

    val messageCapture = ArgumentCaptor.forClass(Bytes::class.java)
    verify(service).send(eq(ETH64), eq(MessageType.BlockHeaders.code), eq(conn), messageCapture.capture())

    val messageRead = BlockHeaders.read(messageCapture.value)
    assertEquals(3, messageRead.headers.size)
    assertEquals(UInt256.ZERO, messageRead.headers[0].number)
    assertEquals(UInt256.valueOf(2), messageRead.headers[1].number)
    assertEquals(UInt256.valueOf(4), messageRead.headers[2].number)
  }

  @Test
  fun testGetHeadersTooMany() = runBlocking {
    val conn = mock(WireConnection::class.java)
    `when`(conn.agreedSubprotocolVersion(eq(ETH62.name()))).thenReturn(ETH64)
    handler.handle(
      conn,
      MessageType.GetBlockHeaders.code,
      GetBlockHeaders(genesisBlock.header.hash, 100, 1, false).toBytes()
    ).await()

    val messageCapture = ArgumentCaptor.forClass(Bytes::class.java)
    verify(service).send(eq(ETH64), eq(MessageType.BlockHeaders.code), eq(conn), messageCapture.capture())

    val messageRead = BlockHeaders.read(messageCapture.value)
    assertEquals(6, messageRead.headers.size)
  }

  @Test
  fun testGetHeadersDifferentSkip() = runBlocking {
    val conn = mock(WireConnection::class.java)
    `when`(conn.agreedSubprotocolVersion(eq(ETH62.name()))).thenReturn(ETH64)
    handler.handle(
      conn,
      MessageType.GetBlockHeaders.code,
      GetBlockHeaders(genesisBlock.header.hash, 100, 2, false).toBytes()
    ).await()

    val messageCapture = ArgumentCaptor.forClass(Bytes::class.java)
    verify(service).send(eq(ETH64), eq(MessageType.BlockHeaders.code), eq(conn), messageCapture.capture())

    val messageRead = BlockHeaders.read(messageCapture.value)
    assertEquals(4, messageRead.headers.size)
    assertEquals(UInt256.valueOf(3), messageRead.headers[1].number)
    assertEquals(UInt256.valueOf(6), messageRead.headers[2].number)
    assertEquals(UInt256.valueOf(9), messageRead.headers[3].number)
  }

  @Test
  fun testGetBodies() = runBlocking {
    val conn = mock(WireConnection::class.java)
    `when`(conn.agreedSubprotocolVersion(eq(ETH62.name()))).thenReturn(ETH64)
    handler.handle(
      conn,
      MessageType.GetBlockBodies.code,
      GetBlockBodies(listOf(genesisBlock.header.hash)).toBytes()
    ).await()

    val messageCapture = ArgumentCaptor.forClass(Bytes::class.java)
    verify(service).send(eq(ETH64), eq(MessageType.BlockBodies.code), eq(conn), messageCapture.capture())

    val messageRead = BlockBodies.read(messageCapture.value)
    assertEquals(1, messageRead.bodies.size)
    assertEquals(genesisBlock.body, messageRead.bodies[0])
  }

  @Test
  fun testGetReceipts() = runBlocking {
    val conn = mock(WireConnection::class.java)
    `when`(conn.agreedSubprotocolVersion(eq(ETH62.name()))).thenReturn(ETH64)
    `when`(conn.agreedSubprotocolVersion(eq(ETH62.name()))).thenReturn(ETH64)
    val hashes = repository.findBlockByHashOrNumber(UInt256.valueOf(7).toBytes())
    val block7 = repository.retrieveBlock(hashes[0])
    val txReceipt = repository.retrieveTransactionReceipt(hashes[0], 0)
    handler.handle(
      conn,
      MessageType.GetReceipts.code,
      GetReceipts(listOf(block7!!.header.hash)).toBytes()
    ).await()

    val messageCapture = ArgumentCaptor.forClass(Bytes::class.java)
    verify(service).send(eq(ETH64), eq(MessageType.Receipts.code), eq(conn), messageCapture.capture())

    val messageRead = Receipts.read(messageCapture.value)
    assertEquals(1, messageRead.transactionReceipts.size)
    assertEquals(txReceipt, messageRead.transactionReceipts[0][0])
  }

  @Test
  fun testGetBodiesEmpty(): Unit = runBlocking {
    val conn = mock(WireConnection::class.java)
    handler.handle(
      conn,
      MessageType.GetBlockBodies.code,
      GetBlockBodies(listOf()).toBytes()
    ).await()

    verify(service).disconnect(conn, DisconnectReason.SUBPROTOCOL_REASON)
  }
}
