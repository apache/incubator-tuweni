/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.les

import net.consensys.cava.eth.repository.BlockchainIndex
import net.consensys.cava.eth.repository.BlockchainRepository
import net.consensys.cava.junit.LuceneIndexWriter
import net.consensys.cava.junit.LuceneIndexWriterExtension
import net.consensys.cava.junit.TempDirectoryExtension
import net.consensys.cava.kv.MapKeyValueStore
import net.consensys.cava.rlpx.wire.SubProtocolIdentifier
import net.consensys.cava.units.bigints.UInt256
import org.apache.lucene.index.IndexWriter
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(TempDirectoryExtension::class, LuceneIndexWriterExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class LESSubprotocolTest {

  @Test
  @Throws(Exception::class)
  fun supportsLESv2(@LuceneIndexWriter writer: IndexWriter) {

    val sp = LESSubprotocol(
      1,
      false,
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
      BlockchainIndex(writer)
      )
    )
    assertTrue(sp.supports(SubProtocolIdentifier.of("les", 2)))
  }

  @Test
  @Throws(Exception::class)
  fun noSupportForv3(@LuceneIndexWriter writer: IndexWriter) {

    val sp = LESSubprotocol(
      1,
      false,
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
      BlockchainIndex(writer)
      )
    )
    assertFalse(sp.supports(SubProtocolIdentifier.of("les", 3)))
  }

  @Test
  @Throws(Exception::class)
  fun noSupportForETH(@LuceneIndexWriter writer: IndexWriter) {
    val sp = LESSubprotocol(
      1,
      false,
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
        BlockchainIndex(writer)
      )
    )
    assertFalse(sp.supports(SubProtocolIdentifier.of("eth", 2)))
  }
}
