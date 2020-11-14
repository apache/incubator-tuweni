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

/**
 * A log entry is a tuple of a logger’s address (the address of the contract that added the logs), a series of 32-bytes
 * log topics, and some number of bytes of data.
 */
public final class Log {

  /**
   * Reads the log entry from the provided RLP input.
   *
   * @param in the input from which to decode the log entry.
   * @return the read log entry.
   */
  public static Log readFrom(final RLPReader in) {
    return in.readList(reader -> {
      final Address logger = Address.fromBytes(reader.readValue());
      final List<Bytes32> topics = reader.readListContents(topicsReader -> Bytes32.wrap(topicsReader.readValue()));
      final Bytes data = reader.readValue();
      return new Log(logger, data, topics);
    });
  }

  private final Address logger;
  private final Bytes data;
  private final List<Bytes32> topics;

  /**
   * @param logger The address of the contract that produced this log.
   * @param data Data associated with this log.
   * @param topics Indexable topics associated with this log.
   */
  public Log(final Address logger, final Bytes data, final List<Bytes32> topics) {
    this.logger = logger;
    this.data = data;
    this.topics = topics;
  }

  /**
   * Writes the log entry to the provided RLP output.
   *
   * @param writer the output in which to encode the log entry.
   */
  public void writeTo(final RLPWriter writer) {
    writer.writeList(out -> {
      out.writeValue(logger);
      out.writeList(topicsWriter -> {
        for (Bytes32 topic : topics) {
          topicsWriter.writeValue(topic);
        }
      });
      out.writeValue(data);
    });
  }

  /**
   * Encodes log to RLP.
   *
   * @return the log as RLP encoded
   */
  public Bytes toBytes() {
    return RLP.encode(this::writeTo);
  }

  /**
   * Provides the logger's address
   * 
   * @return the address of the contract that produced this log.
   */
  public Address getLogger() {
    return logger;
  }

  /**
   * Provides the data of the log
   * 
   * @return data associated with this log.
   */
  public Bytes getData() {
    return data;
  }

  /**
   * Provides the topics of the log
   * 
   * @return indexable topics associated with this log.
   */
  public List<Bytes32> getTopics() {
    return topics;
  }

  @Override
  public boolean equals(final Object other) {
    if (!(other instanceof Log))
      return false;

    final Log that = (Log) other;
    return this.data.equals(that.data) && this.logger.equals(that.logger) && this.topics.equals(that.topics);
  }

  @Override
  public int hashCode() {
    return Objects.hash(data, logger, topics);
  }

  @Override
  public String toString() {
    return "Log{" + "logger=" + logger + ", data=" + data + ", topics=" + topics + '}';
  }
}
