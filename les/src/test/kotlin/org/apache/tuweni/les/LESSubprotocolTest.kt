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

import kotlinx.coroutines.Dispatchers
import org.apache.lucene.index.IndexWriter
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.devp2p.eth.SimpleBlockchainInformation
import org.apache.tuweni.eth.Hash
import org.apache.tuweni.eth.repository.BlockchainIndex
import org.apache.tuweni.eth.repository.BlockchainRepository
import org.apache.tuweni.eth.repository.MemoryTransactionPool
import org.apache.tuweni.junit.LuceneIndexWriter
import org.apache.tuweni.junit.LuceneIndexWriterExtension
import org.apache.tuweni.junit.TempDirectoryExtension
import org.apache.tuweni.kv.MapKeyValueStore
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.units.bigints.UInt256
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TempDirectoryExtension::class, LuceneIndexWriterExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LESSubprotocolTest {

  private val blockchainInformation = SimpleBlockchainInformation(
    UInt256.valueOf(42L),
    UInt256.ONE,
    Hash.fromBytes(Bytes32.random()), UInt256.valueOf(42L),
    Hash.fromBytes(Bytes32.random()),
    emptyList()
  )

  @Test
  @Throws(Exception::class)
  fun supportsLESv2(@LuceneIndexWriter writer: IndexWriter) {

    val sp = LESSubprotocol(
      Dispatchers.Default,
      blockchainInformation,
      true,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      BlockchainRepository(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer)
      ),
      MemoryTransactionPool()
    )
    assertTrue(sp.supports(SubProtocolIdentifier.of("les", 2)))
  }

  @Test
  @Throws(Exception::class)
  fun noSupportForv3(@LuceneIndexWriter writer: IndexWriter) {

    val sp = LESSubprotocol(
      Dispatchers.Default,
      blockchainInformation,
      true,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      BlockchainRepository(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer)
      ),
      MemoryTransactionPool()
    )
    assertFalse(sp.supports(SubProtocolIdentifier.of("les", 3)))
  }

  @Test
  @Throws(Exception::class)
  fun noSupportForETH(@LuceneIndexWriter writer: IndexWriter) {
    val sp = LESSubprotocol(
      Dispatchers.Default,
      blockchainInformation,
      true,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      UInt256.ZERO,
      BlockchainRepository(
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        MapKeyValueStore(),
        BlockchainIndex(writer)
      ),
      MemoryTransactionPool()
    )
    assertFalse(sp.supports(SubProtocolIdentifier.of("eth", 2)))
  }
}
