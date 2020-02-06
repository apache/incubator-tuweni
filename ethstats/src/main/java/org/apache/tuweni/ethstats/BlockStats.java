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
package org.apache.tuweni.ethstats;


import com.fasterxml.jackson.annotation.JsonGetter;
import org.apache.tuweni.eth.Address;
import org.apache.tuweni.eth.BlockHeader;
import org.apache.tuweni.eth.Hash;
import org.apache.tuweni.eth.Transaction;
import org.apache.tuweni.units.bigints.UInt256;

import java.util.List;

public final class BlockStats {

  private final UInt256 blockNumber;
  private final Hash hash;
  private final Hash parentHash;
  private final long timestamp;
  private final Address miner;
  private final long gasUsed;
  private final long gasLimit;
  private final UInt256 difficulty;
  private final UInt256 totalDifficulty;
  private final List<TxStats> transactions;
  private final Hash transactionsRoot;
  private final Hash stateRoot;
  private final List<BlockHeader> uncles;


  BlockStats(UInt256 blockNumber, Hash hash, Hash parentHash, long timestamp, Address miner, long gasUsed, long gasLimit, UInt256 difficulty, UInt256 totalDifficulty, List<TxStats> transactions, Hash transactionsRoot, Hash stateRoot, List<BlockHeader> uncles) {
    this.blockNumber = blockNumber;
    this.hash = hash;
    this.parentHash = parentHash;
    this.timestamp = timestamp;
    this.miner = miner;
    this.gasUsed = gasUsed;
    this.gasLimit = gasLimit;
    this.difficulty = difficulty;
    this.totalDifficulty = totalDifficulty;
    this.transactions = transactions;
    this.transactionsRoot = transactionsRoot;
    this.stateRoot = stateRoot;
    this.uncles = uncles;
  }

  @JsonGetter("number")
  public long getBlockNumber() {
    return blockNumber.toLong();
  }

  @JsonGetter("hash")
  public String getHash() {
    return hash.toHexString();
  }

  @JsonGetter("parentHash")
  public Hash getParentHash() {
    return parentHash;
  }

  @JsonGetter("timestamp")
  public long getTimestamp() {
    return timestamp;
  }

  @JsonGetter("miner")
  public Address getMiner() {
    return miner;
  }

  @JsonGetter("gasUsed")
  public long getGasUsed() {
    return gasUsed;
  }

  @JsonGetter("gasLimit")
  public long getGasLimit() {
    return gasLimit;
  }

  @JsonGetter("difficulty")
  public String getDifficulty() {
    return difficulty.toString();
  }

  @JsonGetter("totalDifficulty")
  public String getTotalDifficulty() {
    return totalDifficulty.toString();
  }

  @JsonGetter("transactions")
  public List<TxStats> getTransactions() {
    return transactions;
  }

  @JsonGetter("transactionsRoot")
  public String getTransactionsRoot() {
    return transactionsRoot.toHexString();
  }

  @JsonGetter("stateRoot")
  public Hash getStateRoot() {
    return stateRoot;
  }

  @JsonGetter("uncles")
  public List<BlockHeader> getUncles() {
    return uncles;
  }
}
