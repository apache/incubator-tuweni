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

import static org.apache.tuweni.crypto.Hash.keccak256;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.bytes.MutableBytes;
import org.apache.tuweni.rlp.RLPReader;

import java.util.Collection;

/**
 * Bloom filter implementation for storing persistent logs, describes a 2048-bit representation of all log entries of a
 * transaction, except data. Sets the bits of the 2048 byte array, where indices are given by: The lower order 11-bits,
 * of the first three double-bytes, of the SHA3, of each value. For instance the address
 * "0x0F572E5295C57F15886F9B263E2F6D2D6C7B5EC6" results in the KECCAK256 hash
 * "bd2b01afcd27800b54d2179edc49e2bffde5078bb6d0b204694169b1643fb108", of which the corresponding double-bytes are:
 * bd2b, 01af, cd27, corresponding to the following bits in the bloom filter: 1323, 431, 1319
 */
public final class LogsBloomFilter {

  private static final int LEAST_SIGNIFICANT_THREE_BITS = 0x7;

  /**
   * Creates a bloom filter corresponding to the provide log series.
   *
   * @param logs the logs to populate the bloom filter with.
   * @return the newly created bloom filter populated with the logs from {@code logs}.
   */
  public static LogsBloomFilter compute(final Collection<Log> logs) {
    final LogsBloomFilter bloom = new LogsBloomFilter();
    logs.forEach(bloom::insertLog);
    return bloom;
  }

  /**
   * Creates a bloom filter from the given RLP-encoded input.
   *
   * @param input The input to read from
   * @return the input's corresponding bloom filter
   */
  public static LogsBloomFilter readFrom(final RLPReader input) {
    return new LogsBloomFilter(input.readValue());
  }

  private final MutableBytes data;

  public LogsBloomFilter() {
    this.data = MutableBytes.create(256);
  }

  public LogsBloomFilter(Bytes data) {
    if (data.size() != 256) {
      throw new IllegalArgumentException(
          String.format("Invalid size for bloom filter backing array: expected 256 but got %s", data.size()));
    }
    this.data = data.mutableCopy();
  }

  public Bytes toBytes() {
    return data;
  }

  public void insertLog(final Log log) {
    setBits(keccak256(log.getLogger()));

    for (final Bytes32 topic : log.getTopics()) {
      setBits(keccak256(topic));
    }
  }

  public void digest(final LogsBloomFilter other) {
    for (int i = 0; i < data.size(); ++i) {
      data.set(i, (byte) ((data.get(i) | other.data.get(i)) & 0xFF));
    }
  }

  /**
   * Discover the low order 11-bits, of the first three double-bytes, of the SHA3 hash, of each value and update the
   * bloom filter accordingly.
   *
   * @param hashValue The hash of the log item.
   */
  private void setBits(final Bytes hashValue) {
    for (int counter = 0; counter < 6; counter += 2) {
      final int setBloomBit =
          ((hashValue.get(counter) & LEAST_SIGNIFICANT_THREE_BITS) << 8) + (hashValue.get(counter + 1) & 0xFF);
      setBit(setBloomBit);
    }
  }

  private void setBit(final int index) {
    final int byteIndex = 256 - 1 - index / 8;
    final int bitIndex = index % 8;
    data.set(byteIndex, (byte) (data.get(byteIndex) | (1 << bitIndex)));
  }

  @Override
  public String toString() {
    return data.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof LogsBloomFilter)) {
      return false;
    }
    final LogsBloomFilter other = (LogsBloomFilter) obj;
    return data.equals(other.data);
  }

  @Override
  public int hashCode() {
    return data.hashCode();
  }
}
