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
package org.apache.tuweni.rlpx;

import org.apache.tuweni.bytes.Bytes;

import java.util.Objects;

/**
 * Message exchanged over a RLPx connection.
 * <p>
 * The message is identified by a negotiated code, offset according to the subprotocol mapping.
 * <p>
 * The message includes the raw content of the message as bytes.
 */
public final class RLPxMessage {

  private final int messageId; // messageId with the proper offset
  private final Bytes content;
  private final int bytesLength;

  public RLPxMessage(int messageId, Bytes content) {
    this(messageId, content, 0);
  }

  RLPxMessage(int messageId, Bytes content, int bytesLength) {
    this.messageId = messageId;
    this.content = content;
    this.bytesLength = bytesLength;
  }

  int bytesLength() {
    return bytesLength;
  }

  /**
   * Message raw content
   * 
   * @return the raw content of the message
   */
  public Bytes content() {
    return content;
  }

  /**
   * Message ID
   * 
   * @return the message ID
   */
  public int messageId() {
    return messageId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    RLPxMessage that = (RLPxMessage) o;
    return messageId == that.messageId && Objects.equals(content, that.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(messageId, content);
  }
}
