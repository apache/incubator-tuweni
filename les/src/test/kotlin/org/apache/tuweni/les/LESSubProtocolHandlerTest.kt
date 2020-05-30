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
package org.apache.tuweni.les

import kotlinx.coroutines.runBlocking
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.Block
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.junit.LuceneIndexWriter
import org.apache.tuweni.junit.LuceneIndexWriterExtension
import org.apache.tuweni.junit.VertxExtension
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.les.LESSubprotocol.Companion.LES_ID
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.WireConnectionRepository
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.apache.lucene.index.IndexWriter
import org.apache.tuweni.concurrent.AsyncResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.net.InetSocketAddress
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(BouncyCastleExtension::class, VertxExtension::class, LuceneIndexWriterExtension::class)
internal class LESSubProtocolHandlerTest @Throws(IOException::class)
constructor() {

  private val header = BlockHeader(
    Hash.fromBytes(Bytes32.random()),
    Hash.fromBytes(Bytes32.random()),
    Address.fromBytes(Bytes.random(20)),
    Hash.fromBytes(Bytes32.random()),
    Hash.fromBytes(Bytes32.random()),
    Hash.fromBytes(Bytes32.random()),
    Bytes32.random(),
    UInt256.fromBytes(Bytes32.random()),
    UInt256.fromBytes(Bytes32.random()),
    Gas.valueOf(3),
    Gas.valueOf(2),
    Instant.now().truncatedTo(ChronoUnit.SECONDS),
    Bytes.of(2, 3, 4),
    Hash.fromBytes(Bytes32.random()),
    Bytes32.random()
  )
  private val body = BlockBody(
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
    emptyList()
  )
  private val block = Block(header, body)

  private class MyRLPxService : RLPxService {

    var message: Bytes? = null
    var disconnectReason: DisconnectReason? = null

    override fun connectTo(peerPublicKey: SECP256K1.PublicKey, peerAddress: InetSocketAddress): AsyncResult<String>? {
      return null
    }

    override fun start(): AsyncCompletion? {
      return null
    }

    override fun stop(): AsyncCompletion? {
      return null
    }

    override fun send(
      subProtocolIdentifier: SubProtocolIdentifier,
      messageType: Int,
      connectionId: String,
      message: Bytes
    ) {
      this.message = message
    }

    override fun broadcast(subProtocolIdentifier: SubProtocolIdentifier, messageType: Int, message: Bytes) {
    }

    override fun disconnect(connectionId: String, reason: DisconnectReason) {
      this.disconnectReason = reason
    }

    override fun repository(): WireConnectionRepository? {
      return null
    }
  }

  @Test
  @Throws(Exception::class)
  fun sendStatusOnNewConnection(@LuceneIndexWriter writer: IndexWriter) =
    runBlocking {
      val service = MyRLPxService()
      val block = Block(header, body)
      val repo = BlockchainRepository
        .init(
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          BlockchainIndex(writer),
          block
        )

      val handler = LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo
      )
      handler.handleNewPeerConnection("abc").await()
      val message = StatusMessage.read(service.message!!)
      assertNotNull(message)
      assertEquals(2, message.protocolVersion)
      assertEquals(UInt256.ZERO, message.flowControlBufferLimit)
      assertEquals(block.getHeader().getHash(), message.genesisHash)
    }

  @Test
  @Throws(Exception::class)
  fun receiveStatusTwice(@LuceneIndexWriter writer: IndexWriter) =
    runBlocking {
      val status = StatusMessage(
        2,
        1,
        UInt256.valueOf(23),
        Bytes32.random(),
        UInt256.valueOf(3443),
        Bytes32.random(), null,
        UInt256.valueOf(333),
        UInt256.valueOf(453),
        true,
        UInt256.valueOf(3),
        UInt256.valueOf(4),
        UInt256.valueOf(5),
        0
      ).toBytes()
      val service = MyRLPxService()

      val repo = BlockchainRepository
        .init(
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          BlockchainIndex(writer),
          block
        )

      val handler = LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo
      )
      handler.handleNewPeerConnection("abc").await()
      handler.handle("abc", 0, status).await()
      assertThrows(IllegalStateException::class.java) { runBlocking {
        handler.handle("abc", 0, status).await()
      } }

      assertEquals(DisconnectReason.PROTOCOL_BREACH, service.disconnectReason)
  }

  @Test
  @Throws(Exception::class)
  fun receiveOtherMessageBeforeStatus(@LuceneIndexWriter writer: IndexWriter) = runBlocking {
    val service = MyRLPxService()
    val repo = BlockchainRepository(
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      MapKeyValueStore(),
      BlockchainIndex(writer)
    )
    val handler = LESSubProtocolHandler(
      service,
      LES_ID,
      1,
      false,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      repo
    )
    assertThrows(IllegalStateException::class.java) { runBlocking {
      handler.handle("abc", 2, Bytes.random(2)).await()
    } }

    assertEquals(DisconnectReason.PROTOCOL_BREACH, service.disconnectReason)
  }

  @Test
  @Throws(Exception::class)
  fun receivedGetBlockHeadersMessage(@LuceneIndexWriter writer: IndexWriter) =
    runBlocking {
      val service = MyRLPxService()
      val repo = BlockchainRepository
        .init(
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          BlockchainIndex(writer),
          block
        )
      val handler = LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo
      )
      val status = StatusMessage(
        2,
        1,
        UInt256.valueOf(23),
        Bytes32.random(),
        UInt256.valueOf(3443),
        Bytes32.random(), null,
        UInt256.valueOf(333),
        UInt256.valueOf(453),
        true,
        UInt256.valueOf(3),
        UInt256.valueOf(4),
        UInt256.valueOf(5),
        0
      ).toBytes()
      handler.handleNewPeerConnection("abc").await()
      handler.handle("abc", 0, status).await()

      handler.handle(
        "abc",
        2,
        GetBlockHeadersMessage(
          1,
          listOf(
            GetBlockHeadersMessage.BlockHeaderQuery(
              Bytes32.random(),
              UInt256.valueOf(3),
              UInt256.valueOf(0),
              GetBlockHeadersMessage.BlockHeaderQuery.Direction.BACKWARDS
            )
          )
        ).toBytes()
      ).await()
      val blockHeaders = BlockHeadersMessage.read(service.message!!)
      assertTrue(blockHeaders.blockHeaders.isEmpty())
  }

  @Test
  @Throws(Exception::class)
  fun receivedBlockHeadersMessage(@LuceneIndexWriter writer: IndexWriter) =
    runBlocking {
      val service = MyRLPxService()
      val repo = BlockchainRepository
        .init(
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          BlockchainIndex(writer),
          block
        )
      val handler = LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo
      )
      val status = StatusMessage(
        2,
        1,
        UInt256.valueOf(23),
        Bytes32.random(),
        UInt256.valueOf(3443),
        Bytes32.random(), null,
        UInt256.valueOf(333),
        UInt256.valueOf(453),
        true,
        UInt256.valueOf(3),
        UInt256.valueOf(4),
        UInt256.valueOf(5),
        0
      ).toBytes()

      val header = BlockHeader(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Address.fromBytes(Bytes.random(20)),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random(),
        UInt256.fromBytes(Bytes32.random()),
        UInt256.fromBytes(Bytes32.random()),
        Gas.valueOf(3),
        Gas.valueOf(2),
        Instant.now().truncatedTo(ChronoUnit.SECONDS),
        Bytes.of(2, 3, 4),
        Hash.fromBytes(Bytes32.random()),
        Bytes32.random()
      )

      handler.handleNewPeerConnection("abc").await()
      handler.handle("abc", 0, status).await()
      handler.handle("abc", 3, BlockHeadersMessage(1, 2, listOf(header)).toBytes()).await()
      val retrieved = repo.retrieveBlockHeader(header.getHash())
      assertEquals(header, retrieved)
  }

  @Test
  @Throws(Exception::class)
  fun receivedGetBlockBodiesMessage(@LuceneIndexWriter writer: IndexWriter) =
    runBlocking {
      val service = MyRLPxService()
      val repo = BlockchainRepository
        .init(
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          BlockchainIndex(writer),
          block
        )
      val handler = LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo
      )
      val status = StatusMessage(
        2,
        1,
        UInt256.valueOf(23),
        Bytes32.random(),
        UInt256.valueOf(3443),
        Bytes32.random(), null,
        UInt256.valueOf(333),
        UInt256.valueOf(453),
        true,
        UInt256.valueOf(3),
        UInt256.valueOf(4),
        UInt256.valueOf(5),
        0
      ).toBytes()
      handler.handleNewPeerConnection("abc").await()
      handler.handle("abc", 0, status).await()

      handler
        .handle("abc", 4, GetBlockBodiesMessage(1, listOf(Hash.fromBytes(Bytes32.random()))).toBytes()).await()
      val received = service.message
      val blockBodies = BlockBodiesMessage.read(received!!)
      assertTrue(blockBodies.blockBodies.isEmpty())
  }

  @Test
  @Throws(Exception::class)
  fun receivedGetReceiptsMessage(@LuceneIndexWriter writer: IndexWriter) =
    runBlocking {
      val service = MyRLPxService()
      val repo = BlockchainRepository
        .init(
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          MapKeyValueStore(),
          BlockchainIndex(writer),
          block
        )
      val handler = LESSubProtocolHandler(
        service,
        LES_ID,
        1,
        false,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        UInt256.ZERO,
        repo
      )
      val status = StatusMessage(
        2,
        1,
        UInt256.valueOf(23),
        Bytes32.random(),
        UInt256.valueOf(3443),
        Bytes32.random(), null,
        UInt256.valueOf(333),
        UInt256.valueOf(453),
        true,
        UInt256.valueOf(3),
        UInt256.valueOf(4),
        UInt256.valueOf(5),
        0
      ).toBytes()
      handler.handleNewPeerConnection("abc").await()
      handler.handle("abc", 0, status).await()

      handler
        .handle("abc", 4, GetReceiptsMessage(1, listOf(Hash.fromBytes(Bytes32.random()))).toBytes()).await()
      val received = service.message
      val receipts = ReceiptsMessage.read(received!!)
      assertTrue(receipts.receipts.isEmpty())
    }
}
