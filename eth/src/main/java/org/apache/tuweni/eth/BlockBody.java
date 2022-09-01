/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.eth;

import static java.util.Objects.requireNonNull;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.rlp.RLP;
import org.apache.tuweni.rlp.RLPException;
import org.apache.tuweni.rlp.RLPReader;
import org.apache.tuweni.rlp.RLPWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An Ethereum block body.
 */
public final class BlockBody {

  /**
   * Deserialize a block body from RLP encoded bytes.
   *
   * @param encoded The RLP encoded block.
   * @return The deserialized block body.
   * @throws RLPException If there is an error decoding the block body.
   */
  public static BlockBody fromBytes(Bytes encoded) {
    requireNonNull(encoded);
    return RLP.decodeList(encoded, BlockBody::readFrom);
  }

  public static BlockBody readFrom(RLPReader reader) {
    List<Transaction> txs = new ArrayList<>();
    reader.readList((listReader, l) -> {
      while (!listReader.isComplete()) {
        txs.add(listReader.readList(Transaction::readFrom));
      }
    });
    List<BlockHeader> ommers = new ArrayList<>();
    reader.readList((listReader, l) -> {
      while (!listReader.isComplete()) {
        ommers.add(listReader.readList(BlockHeader::readFrom));
      }
    });

    return new BlockBody(txs, ommers);
  }

  private final List<Transaction> transactions;
  private final List<BlockHeader> ommers;

  /**
   * Creates a new block body.
   *
   * @param transactions the list of transactions in this block.
   * @param ommers the list of ommers for this block.
   */
  public BlockBody(List<Transaction> transactions, List<BlockHeader> ommers) {
    requireNonNull(transactions);
    requireNonNull(ommers);
    this.transactions = transactions;
    this.ommers = ommers;
  }

  /**
   * Provides the block transactions
   * 
   * @return the transactions of the block.
   */
  public List<Transaction> getTransactions() {
    return transactions;
  }

  /**
   * Provides the block ommers
   * 
   * @return the list of ommers for this block.
   */
  public List<BlockHeader> getOmmers() {
    return ommers;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof BlockBody)) {
      return false;
    }
    BlockBody other = (BlockBody) obj;
    return transactions.equals(other.transactions) && ommers.equals(other.ommers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(transactions, ommers);
  }

  /**
   * Provides the block body bytes
   * 
   * @return The RLP serialized form of this block body.
   */
  public Bytes toBytes() {
    return RLP.encodeList(this::writeTo);
  }

  @Override
  public String toString() {
    return "BlockBody{" + "transactions=" + transactions + ", ommers=" + ommers + '}';
  }

  public void writeTo(RLPWriter writer) {
    writer.writeList(listWriter -> {
      for (Transaction tx : transactions) {
        listWriter.writeList(tx::writeTo);
      }
    });
    writer.writeList(listWriter -> {
      for (BlockHeader ommer : ommers) {
        listWriter.writeList(ommer::writeTo);
      }
    });
  }
}
