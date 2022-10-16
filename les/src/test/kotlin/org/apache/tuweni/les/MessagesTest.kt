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

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.eth.Status
import org.apache.tuweni.eth.Address
import org.apache.tuweni.eth.BlockBody
import org.apache.tuweni.eth.BlockHeader
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.Log
import org.apache.tuweni.eth.LogsBloomFilter
import org.apache.tuweni.eth.Transaction
import org.apache.tuweni.eth.TransactionReceipt
import org.apache.tuweni.junit.BouncyCastleExtension
import org.apache.tuweni.units.bigints.UInt256
import org.apache.tuweni.units.bigints.UInt64
import org.apache.tuweni.units.ethereum.Gas
import org.apache.tuweni.units.ethereum.Wei
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.time.temporal.ChronoUnit

@ExtendWith(BouncyCastleExtension::class)
internal class BlockBodiesMessageTest {

  @Test
  fun roundtripRLP() {
    val message = BlockBodiesMessage(
      3,
      2,
      listOf(
        BlockBody(
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
          emptyList<BlockHeader>()
        )
      )
    )
    val rlp = message.toBytes()
    val read = BlockBodiesMessage.read(rlp)
    assertEquals(message, read)
  }
}

@ExtendWith(BouncyCastleExtension::class)
internal class ReceiptsMessageTest {

  @Test
  fun roundtripRLP() {
    val logsList = listOf(
      Log(
        Address.fromBytes(Bytes.random(20)),
        Bytes.of(1, 2, 3),
        listOf(Bytes32.random(), Bytes32.random(), Bytes32.random())
      ),
      Log(
        Address.fromBytes(Bytes.random(20)),
        Bytes.of(1, 2, 3),
        listOf(Bytes32.random(), Bytes32.random(), Bytes32.random())
      ),
      Log(
        Address.fromBytes(Bytes.random(20)),
        Bytes.of(1, 2, 3),
        listOf(Bytes32.random(), Bytes32.random(), Bytes32.random())
      )
    )
    val message = ReceiptsMessage(
      3,
      2,
      listOf(
        listOf(
          TransactionReceipt(
            Bytes32.random(),
            3,
            LogsBloomFilter.compute(
              logsList
            ),
            logsList
          )
        )
      )
    )
    val rlp = message.toBytes()
    val read = ReceiptsMessage.read(rlp)
    assertEquals(message, read)
  }
}

internal class BlockHeadersMessageTest {

  @Test
  fun roundtripRLP() {
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
      UInt64.random()
    )
    val message = BlockHeadersMessage(3L, 2L, listOf(header))
    val bytes = message.toBytes()
    assertEquals(message, BlockHeadersMessage.read(bytes))
  }
}

internal class GetBlockBodiesMessageTest {

  @Test
  fun roundtripRLP() {
    val message = GetBlockBodiesMessage(
      3,
      listOf(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random())
      )
    )
    val rlp = message.toBytes()
    val read = GetBlockBodiesMessage.read(rlp)
    assertEquals(message, read)
  }
}

internal class GetReceiptsMessageTest {

  @Test
  fun roundtripRLP() {
    val message = GetReceiptsMessage(
      3,
      listOf(
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random()),
        Hash.fromBytes(Bytes32.random())
      )
    )
    val rlp = message.toBytes()
    val read = GetReceiptsMessage.read(rlp)
    assertEquals(message, read)
  }
}

internal class GetBlockHeadersMessageTest {

  @Test
  fun roundtripBytes() {
    val message = GetBlockHeadersMessage(
      344,
      listOf(
        GetBlockHeadersMessage.BlockHeaderQuery(
          Bytes32.random(),
          UInt256.valueOf(32),
          UInt256.valueOf(64),
          GetBlockHeadersMessage.BlockHeaderQuery.Direction.BACKWARDS
        ),
        GetBlockHeadersMessage.BlockHeaderQuery(
          Bytes32.random(),
          UInt256.valueOf(32),
          UInt256.valueOf(64),
          GetBlockHeadersMessage.BlockHeaderQuery.Direction.FORWARD
        ),
        GetBlockHeadersMessage.BlockHeaderQuery(
          Bytes32.random(),
          UInt256.valueOf(32),
          UInt256.valueOf(64),
          GetBlockHeadersMessage.BlockHeaderQuery.Direction.BACKWARDS
        )
      )
    )

    val bytes = message.toBytes()
    assertEquals(message, GetBlockHeadersMessage.read(bytes))
  }
}

internal class StatusMessageTest {

  @Test
  fun testStatusMessageRoundtrip() {
    val message = StatusMessage(
      2,
      UInt256.valueOf(1),
      UInt256.valueOf(23),
      Bytes32.random(),
      UInt256.valueOf(3443),
      Bytes32.random(),
      null,
      UInt256.valueOf(333),
      UInt256.valueOf(453),
      true,
      UInt256.valueOf(3),
      UInt256.valueOf(4),
      UInt256.valueOf(5),
      1
    )
    val read = StatusMessage.read(message.toBytes())
    assertEquals(message, read)
  }

  @Test
  fun testToStatus() {
    val hash = Bytes32.random()
    val genesisHash = Bytes32.random()
    val message = StatusMessage(
      2,
      UInt256.valueOf(1),
      UInt256.valueOf(23),
      hash,
      UInt256.valueOf(3443),
      genesisHash,
      null,
      UInt256.valueOf(333),
      UInt256.valueOf(453),
      true,
      UInt256.valueOf(3),
      UInt256.valueOf(4),
      UInt256.valueOf(5),
      1
    )
    assertEquals(Status(2, UInt256.valueOf(1), UInt256.valueOf(23), hash, genesisHash, null, null), message.toStatus())
  }
}
