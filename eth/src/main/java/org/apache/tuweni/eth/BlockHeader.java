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
import org.apache.tuweni.rlp.RLPReader;
import org.apache.tuweni.rlp.RLPWriter;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.ethereum.Gas;

import java.time.Instant;
import javax.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.google.common.base.Objects;

/**
 * An Ethereum block header.
 */
public final class BlockHeader {

  /**
   * Deserialize a block header from RLP encoded bytes.
   *
   * @param encoded The RLP encoded block.
   * @return The deserialized block header.
   */
  public static BlockHeader fromBytes(Bytes encoded) {
    requireNonNull(encoded);
    return RLP.decodeList(encoded, BlockHeader::readFrom);
  }

  /**
   * Deserialize a block header from an RLP input.
   *
   * @param reader The RLP reader.
   * @return The deserialized block header.
   */
  public static BlockHeader readFrom(RLPReader reader) {
    Bytes parentHashBytes = reader.readValue();
    return new BlockHeader(
        parentHashBytes.isEmpty() ? null : Hash.fromBytes(parentHashBytes),
        Hash.fromBytes(reader.readValue()),
        Address.fromBytes(reader.readValue()),
        Hash.fromBytes(reader.readValue()),
        Hash.fromBytes(reader.readValue()),
        Hash.fromBytes(reader.readValue()),
        reader.readValue(),
        UInt256.fromBytes(reader.readValue()),
        UInt256.fromBytes(reader.readValue()),
        Gas.valueOf(reader.readUInt256()),
        Gas.valueOf(reader.readUInt256()),
        Instant.ofEpochSecond(reader.readLong()),
        reader.readValue(),
        Hash.fromBytes(reader.readValue()),
        reader.readValue());
  }

  @Nullable
  private final Hash parentHash;
  private final Hash ommersHash;
  private final Address coinbase;
  private final Hash stateRoot;
  private final Hash transactionsRoot;
  private final Hash receiptsRoot;
  private final Bytes logsBloom;
  private final UInt256 difficulty;
  private final UInt256 number;
  private final Gas gasLimit;
  private final Gas gasUsed;
  private final Instant timestamp;
  private final Bytes extraData;
  private final Hash mixHash;
  private final Bytes nonce;
  private Hash hash;

  /**
   * Creates a new block header.
   *
   * @param parentHash the parent hash, or null.
   * @param ommersHash the ommers hash.
   * @param coinbase the block's beneficiary address.
   * @param stateRoot the hash associated with the state tree.
   * @param transactionsRoot the hash associated with the transactions tree.
   * @param receiptsRoot the hash associated with the transaction receipts tree.
   * @param logsBloom the bloom filter of the logs of the block.
   * @param difficulty the difficulty of the block.
   * @param number the number of the block.
   * @param gasLimit the gas limit of the block.
   * @param gasUsed the gas used for the block.
   * @param timestamp the timestamp of the block.
   * @param extraData the extra data stored with the block.
   * @param mixHash the hash associated with computional work on the block.
   * @param nonce the nonce of the block.
   */
  public BlockHeader(
      @Nullable Hash parentHash,
      Hash ommersHash,
      Address coinbase,
      Hash stateRoot,
      Hash transactionsRoot,
      Hash receiptsRoot,
      Bytes logsBloom,
      UInt256 difficulty,
      UInt256 number,
      Gas gasLimit,
      Gas gasUsed,
      Instant timestamp,
      Bytes extraData,
      Hash mixHash,
      Bytes nonce) {
    requireNonNull(ommersHash);
    requireNonNull(coinbase);
    requireNonNull(stateRoot);
    requireNonNull(transactionsRoot);
    requireNonNull(receiptsRoot);
    requireNonNull(logsBloom);
    requireNonNull(difficulty);
    requireNonNull(number);
    requireNonNull(gasLimit);
    requireNonNull(gasUsed);
    requireNonNull(timestamp);
    requireNonNull(extraData);
    requireNonNull(mixHash);
    requireNonNull(nonce);
    this.parentHash = parentHash;
    this.ommersHash = ommersHash;
    this.coinbase = coinbase;
    this.stateRoot = stateRoot;
    this.transactionsRoot = transactionsRoot;
    this.receiptsRoot = receiptsRoot;
    this.logsBloom = logsBloom;
    this.difficulty = difficulty;
    this.number = number;
    this.gasLimit = gasLimit;
    this.gasUsed = gasUsed;
    this.timestamp = timestamp;
    this.extraData = extraData;
    this.mixHash = mixHash;
    this.nonce = nonce;
  }

  /**
   * @return the block's beneficiary's address.
   */
  @JsonGetter("miner")
  public Address coinbase() {
    return coinbase;
  }

  /**
   * @return the difficulty of the block.
   */
  @JsonGetter("difficulty")
  public UInt256 difficulty() {
    return difficulty;
  }

  /**
   * @return the extra data stored with the block.
   */
  @JsonGetter("extraData")
  public Bytes extraData() {
    return extraData;
  }

  /**
   * @return the gas limit of the block.
   */
  @JsonGetter("gasLimit")
  public Gas gasLimit() {
    return gasLimit;
  }

  /**
   * @return the gas used for the block.
   */
  @JsonGetter("gasUsed")
  public Gas gasUsed() {
    return gasUsed;
  }

  /**
   * @return the hash of the block header.
   */
  public Hash hash() {
    if (hash == null) {
      Bytes rlp = toBytes();
      hash = Hash.hash(rlp);
    }
    return hash;
  }

  /**
   * @return the bloom filter of the logs of the block.
   */
  @JsonGetter("logsBloom")
  public Bytes logsBloom() {
    return logsBloom;
  }

  /**
   * @return the hash associated with computional work on the block.
   */
  @JsonGetter("mixHash")
  public Hash mixHash() {
    return mixHash;
  }

  /**
   * @return the nonce of the block.
   */
  @JsonGetter("nonce")
  public Bytes nonce() {
    return nonce;
  }

  /**
   * @return the number of the block.
   */
  @JsonGetter("number")
  public UInt256 number() {
    return number;
  }

  /**
   * @return the ommer hash.
   */
  @JsonGetter("sha3Uncles")
  public Hash ommersHash() {
    return ommersHash;
  }

  /**
   * @return the parent hash, or null if none was available.
   */
  @Nullable
  @JsonGetter("parentHash")
  public Hash parentHash() {
    return parentHash;
  }

  /**
   * @return the hash associated with the transaction receipts tree.
   */
  @JsonGetter("receiptsRoot")
  public Hash receiptsRoot() {
    return receiptsRoot;
  }

  /**
   * @return the hash associated with the state tree.
   */
  @JsonGetter("stateRoot")
  public Hash stateRoot() {
    return stateRoot;
  }

  /**
   * @return the timestamp of the block.
   */
  @JsonGetter("timestamp")
  public Instant timestamp() {
    return timestamp;
  }

  /**
   * @return the hash associated with the transactions tree.
   */
  @JsonGetter("transactionsRoot")
  public Hash transactionsRoot() {
    return transactionsRoot;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof BlockHeader)) {
      return false;
    }
    BlockHeader other = (BlockHeader) obj;
    return Objects.equal(parentHash, other.parentHash)
        && ommersHash.equals(other.ommersHash)
        && coinbase.equals(other.coinbase)
        && stateRoot.equals(other.stateRoot)
        && transactionsRoot.equals(other.transactionsRoot)
        && receiptsRoot.equals(other.receiptsRoot)
        && logsBloom.equals(other.logsBloom)
        && difficulty.equals(other.difficulty)
        && number.equals(other.number)
        && gasLimit.equals(other.gasLimit)
        && gasUsed.equals(other.gasUsed)
        && timestamp.equals(other.timestamp)
        && extraData.equals(other.extraData)
        && mixHash.equals(other.mixHash)
        && nonce.equals(other.nonce);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(
        parentHash,
        ommersHash,
        coinbase,
        stateRoot,
        transactionsRoot,
        receiptsRoot,
        logsBloom,
        difficulty,
        number,
        gasLimit,
        gasUsed,
        timestamp,
        extraData,
        mixHash,
        nonce);
  }

  @Override
  public String toString() {
    return "BlockHeader{"
        + "parentHash="
        + parentHash
        + ", ommersHash="
        + ommersHash
        + ", coinbase="
        + coinbase
        + ", stateRoot="
        + stateRoot
        + ", transactionsRoot="
        + transactionsRoot
        + ", receiptsRoot="
        + receiptsRoot
        + ", logsBloom="
        + logsBloom
        + ", difficulty="
        + difficulty
        + ", number="
        + number
        + ", gasLimit="
        + gasLimit
        + ", gasUsed="
        + gasUsed
        + ", timestamp="
        + timestamp
        + ", extraData="
        + extraData
        + ", mixHash="
        + mixHash
        + ", nonce="
        + nonce
        + '}';
  }

  /**
   * @return The RLP serialized form of this block header.
   */
  public Bytes toBytes() {
    return RLP.encodeList(this::writeTo);
  }

  /**
   * Write this block header to an RLP output.
   *
   * @param writer The RLP writer.
   */
  void writeTo(RLPWriter writer) {
    writer.writeValue((parentHash != null) ? parentHash.toBytes() : Bytes.EMPTY);
    writer.writeValue(ommersHash.toBytes());
    writer.writeValue(coinbase.toBytes());
    writer.writeValue(stateRoot.toBytes());
    writer.writeValue(transactionsRoot.toBytes());
    writer.writeValue(receiptsRoot.toBytes());
    writer.writeValue(logsBloom);
    writer.writeValue(difficulty.toMinimalBytes());
    writer.writeValue(number.toMinimalBytes());
    writer.writeValue(gasLimit.toMinimalBytes());
    writer.writeValue(gasUsed.toMinimalBytes());
    writer.writeLong(timestamp.getEpochSecond());
    writer.writeValue(extraData);
    writer.writeValue(mixHash.toBytes());
    writer.writeValue(nonce);
  }
}
