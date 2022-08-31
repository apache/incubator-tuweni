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

import java.util.Objects;

/**
 * An Ethereum block.
 */
public final class Block {

  /**
   * Deserialize a block from RLP encoded bytes.
   *
   * @param encoded The RLP encoded block.
   * @return The deserialized block.
   * @throws RLPException If there is an error decoding the block.
   */
  public static Block fromBytes(Bytes encoded) {
    return RLP.decodeList(encoded, Block::readFrom);
  }

  /**
   * Parse a hexadecimal string into a {@link Block}.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x".
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation.
   * @throws RLPException If there is an error decoding the block.
   */
  public static Block fromHexString(String str) {
    return RLP.decodeList(Bytes.fromHexString(str), Block::readFrom);
  }

  /**
   * Deserialize a block from an RLP input.
   *
   * @param reader The RLP reader.
   * @return The deserialized block.
   * @throws RLPException If there is an error decoding the block.
   */
  public static Block readFrom(RLPReader reader) {
    BlockHeader header = reader.readList(BlockHeader::readFrom);
    BlockBody body = BlockBody.readFrom(reader);
    return new Block(header, body);
  }

  private final BlockHeader header;
  private final BlockBody body;

  /**
   * Creates a block.
   *
   * @param header the header of the block.
   * @param body the body of the block.
   */
  public Block(BlockHeader header, BlockBody body) {
    requireNonNull(header);
    requireNonNull(body);
    this.header = header;
    this.body = body;
  }

  /**
   * Provides the block body
   * 
   * @return the block body.
   */
  public BlockBody getBody() {
    return body;
  }

  /**
   * Provides the block header
   * 
   * @return the block header.
   */
  public BlockHeader getHeader() {
    return header;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Block)) {
      return false;
    }
    Block other = (Block) obj;
    return Objects.equals(header, other.header) && Objects.equals(body, other.body);
  }

  @Override
  public int hashCode() {
    return Objects.hash(header, body);
  }

  @Override
  public String toString() {
    return "Block{" + "header=" + header + ", body=" + body + '}';
  }

  /**
   * Provides the block bytes
   * 
   * @return The RLP serialized form of this block.
   */
  public Bytes toBytes() {
    return RLP.encodeList(this::writeTo);
  }

  /**
   * Write this block to an RLP output.
   *
   * @param writer The RLP writer.
   */
  public void writeTo(RLPWriter writer) {
    writer.writeList(header::writeTo);
    body.writeTo(writer);
  }
}
