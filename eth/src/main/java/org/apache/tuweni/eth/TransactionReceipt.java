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

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.rlp.RLP;
import org.apache.tuweni.rlp.RLPReader;
import org.apache.tuweni.rlp.RLPWriter;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

/**
 * A transaction receipt, containing information pertaining a transaction execution.
 *
 * <p>
 * Transaction receipts have two different formats: state root-encoded and status-encoded. The difference between these
 * two formats is that the state root-encoded transaction receipt contains the state root for world state after the
 * transaction has been processed (e.g. not invalid) and the status-encoded transaction receipt instead has contains the
 * status of the transaction (e.g. 1 for success and 0 for failure). The other transaction receipt fields are the same
 * for both formats: logs, logs bloom, and cumulative gas used in the block. The TransactionReceiptType attribute is the
 * best way to check which format has been used.
 */
public final class TransactionReceipt {

  /**
   * Read a transaction receipt from its RLP serialized representation
   *
   * @param bytes the bytes of the serialized transaction receipt
   * @return a transaction receipt
   */
  public static TransactionReceipt fromBytes(Bytes bytes) {
    return RLP.decode(bytes, TransactionReceipt::readFrom);
  }

  /**
   * Creates a transaction receipt for the given RLP
   *
   * @param reader the RLP-encoded transaction receipt
   * @return the transaction receipt
   */
  public static TransactionReceipt readFrom(final RLPReader reader) {
    return reader.readList(input -> {

      Bytes statusOrRootState = input.readValue();
      long cumulativeGas = input.readLong();
      LogsBloomFilter bloomFilter = new LogsBloomFilter(input.readValue());
      List<Log> logs = input.readListContents(Log::readFrom);

      if (statusOrRootState.size() == 32) {
        return new TransactionReceipt(Bytes32.wrap(statusOrRootState), cumulativeGas, bloomFilter, logs);
      } else {
        int status = statusOrRootState.toInt();
        return new TransactionReceipt(status, cumulativeGas, bloomFilter, logs);
      }
    });
  }

  private final Bytes32 stateRoot;
  private final long cumulativeGasUsed;
  private final List<Log> logs;
  private final LogsBloomFilter bloomFilter;
  private final Integer status;

  /**
   * Creates an instance of a state root-encoded transaction receipt.
   *
   * @param stateRoot the state root for the world state after the transaction has been processed
   * @param cumulativeGasUsed the total amount of gas consumed in the block after this transaction
   * @param bloomFilter the bloom filter of the logs
   * @param logs the logs generated within the transaction
   */
  public TransactionReceipt(Bytes32 stateRoot, long cumulativeGasUsed, LogsBloomFilter bloomFilter, List<Log> logs) {
    this(stateRoot, null, cumulativeGasUsed, bloomFilter, logs);
  }

  /**
   * Creates an instance of a status-encoded transaction receipt.
   *
   * @param status the status code for the transaction (1 for success and 0 for failure)
   * @param cumulativeGasUsed the total amount of gas consumed in the block after this transaction
   * @param bloomFilter the bloom filter of the logs
   * @param logs the logs generated within the transaction
   */
  public TransactionReceipt(int status, long cumulativeGasUsed, LogsBloomFilter bloomFilter, List<Log> logs) {
    this(null, status, cumulativeGasUsed, bloomFilter, logs);
  }

  private TransactionReceipt(
      @Nullable Bytes32 stateRoot,
      @Nullable Integer status,
      long cumulativeGasUsed,
      LogsBloomFilter bloomFilter,
      List<Log> logs) {
    this.stateRoot = stateRoot;
    this.cumulativeGasUsed = cumulativeGasUsed;
    this.status = status;
    this.logs = logs;
    this.bloomFilter = bloomFilter;
  }

  /**
   * Writes the transaction receipt into a serialized RLP form.
   * 
   * @return the transaction receipt encoded as a set of bytes using the RLP serialization
   */
  public Bytes toBytes() {
    return RLP.encode(this::writeTo);
  }

  /**
   * Write an RLP representation.
   *
   * @param writer The RLP output to write to
   */
  public void writeTo(final RLPWriter writer) {
    writer.writeList(out -> {

      // Determine whether it's a state root-encoded transaction receipt
      // or is a status code-encoded transaction receipt.
      if (stateRoot != null) {
        out.writeValue(stateRoot);
      } else {
        out.writeLong(status);
      }
      out.writeLong(cumulativeGasUsed);
      out.writeValue(bloomFilter.toBytes());
      out.writeList(logs, (logWriter, log) -> log.writeTo(logWriter));
    });
  }

  /**
   * Returns the state root for a state root-encoded transaction receipt
   *
   * @return the state root if the transaction receipt is state root-encoded; otherwise {@code null}
   */
  public Bytes32 getStateRoot() {
    return stateRoot;
  }

  /**
   * Returns the total amount of gas consumed in the block after the transaction has been processed.
   *
   * @return the total amount of gas consumed in the block after the transaction has been processed
   */
  public long getCumulativeGasUsed() {
    return cumulativeGasUsed;
  }

  /**
   * Returns the logs generated by the transaction.
   *
   * @return the logs generated by the transaction
   */
  public List<Log> getLogs() {
    return logs;
  }

  /**
   * Returns the logs bloom filter for the logs generated by the transaction
   *
   * @return the logs bloom filter for the logs generated by the transaction
   */
  public LogsBloomFilter getBloomFilter() {
    return bloomFilter;
  }

  /**
   * Computes the logs bloom filters of the current receipt and compares it to the bloom filter stored.
   *
   * @return true if the computed bloom filter matches the bloom filter stored
   */
  public boolean isValid() {
    return LogsBloomFilter.compute(logs).equals(bloomFilter);
  }

  /**
   * Returns the status code for the status-encoded transaction receipt
   *
   * @return the status code if the transaction receipt is status-encoded; otherwise {@code null}
   */
  public Integer getStatus() {
    return status;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof TransactionReceipt)) {
      return false;
    }
    final TransactionReceipt other = (TransactionReceipt) obj;
    return logs.equals(other.logs)
        && Objects.equals(stateRoot, other.stateRoot)
        && cumulativeGasUsed == other.cumulativeGasUsed
        && Objects.equals(status, other.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(logs, stateRoot, cumulativeGasUsed);
  }

  @Override
  public String toString() {
    return "TransactionReceipt{"
        + "stateRoot="
        + stateRoot
        + ", cumulativeGasUsed="
        + cumulativeGasUsed
        + ", logs="
        + logs
        + ", bloomFilter="
        + bloomFilter
        + ", status="
        + status
        + '}';
  }
}
