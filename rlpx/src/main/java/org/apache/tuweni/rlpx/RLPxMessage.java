// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
