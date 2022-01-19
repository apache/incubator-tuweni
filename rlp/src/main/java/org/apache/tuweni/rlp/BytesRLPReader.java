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
package org.apache.tuweni.rlp;

import org.apache.tuweni.bytes.Bytes;

import java.util.function.Function;

final class BytesRLPReader implements RLPReader {

  private final Bytes content;
  private boolean lenient;
  private int index = 0;

  BytesRLPReader(Bytes content, boolean lenient) {
    this.content = content;
    this.lenient = lenient;
  }

  @Override
  public boolean isLenient() {
    return lenient;
  }

  @Override
  public Bytes readRemaining() {
    int remaining = content.size() - index;
    if (remaining == 0) {
      return Bytes.EMPTY;
    }
    return content.slice(index++, remaining);
  }

  @Override
  public Bytes readValue(boolean lenient) {
    int remaining = content.size() - index;
    if (remaining == 0) {
      throw new EndOfRLPException();
    }
    int prefix = (((int) content.get(index)) & 0xFF);
    if (prefix <= 0x7f) {
      return content.slice(index++, 1);
    }
    remaining--;

    if (prefix <= 0xb7) {
      int length = prefix - 0x80;
      if (remaining < length) {
        throw new InvalidRLPEncodingException(
            "Insufficient bytes in RLP encoding: expected " + length + " but have only " + remaining);
      }
      Bytes bytes = content.slice(index + 1, length);
      if (!lenient && length == 1 && (bytes.get(0) & 0xFF) <= 0x7f) {
        throw new InvalidRLPEncodingException("Value should have been encoded as a single byte " + bytes.toHexString());
      }
      index += 1 + length;
      return bytes;
    }
    if (prefix <= 0xbf) {
      int lengthOfLength = prefix - 0xb7;
      if (remaining < lengthOfLength) {
        throw new InvalidRLPEncodingException(
            "Insufficient bytes in RLP encoding: expected " + lengthOfLength + " but have only " + remaining);
      }

      remaining -= lengthOfLength;
      int length = getLength(lengthOfLength, lenient, "value");

      if (remaining < length) {
        throw new InvalidRLPEncodingException(
            "Insufficient bytes in RLP encoding: expected " + length + " but have only " + remaining);
      }

      index += 1 + lengthOfLength;
      Bytes bytes = content.slice(index, length);
      index += length;
      return bytes;
    }
    throw new InvalidRLPTypeException("Attempted to read a value but next item is a list");
  }

  @Override
  public boolean nextIsList() {
    int remaining = content.size() - index;
    if (remaining == 0) {
      throw new EndOfRLPException();
    }
    int prefix = (((int) content.get(index)) & 0xFF);
    return prefix > 0xbf;
  }

  @Override
  public boolean nextIsEmpty() {
    int remaining = content.size() - index;
    if (remaining == 0) {
      throw new EndOfRLPException();
    }
    int prefix = (((int) content.get(index)) & 0xFF);
    return prefix == 0x80;
  }

  @Override
  public <T> T readList(boolean lenient, Function<RLPReader, T> fn) {
    return fn.apply(new BytesRLPReader(readList(lenient), lenient));
  }

  @Override
  public void skipNext(boolean lenient) {
    int remaining = content.size() - index;
    if (remaining == 0) {
      throw new EndOfRLPException();
    }
    int prefix = (((int) content.get(index)) & 0xFF);
    if (prefix <= 0x7f) {
      index++;
      return;
    }
    remaining--;

    if (prefix <= 0xb7) {
      int length = prefix - 0x80;
      if (remaining < length) {
        throw new InvalidRLPEncodingException(
            "Insufficient bytes in RLP encoding: expected " + length + " but have only " + remaining);
      }
      if (!lenient && length == 1 && (content.get(index + 1) & 0xFF) <= 0x7f) {
        throw new InvalidRLPEncodingException(
            "Value should have been encoded as a single byte " + content.slice(index + 1, 1).toHexString());
      }
      index += 1 + length;
      return;
    }
    if (prefix <= 0xbf) {
      int lengthOfLength = prefix - 0xb7;
      if (remaining < lengthOfLength) {
        throw new InvalidRLPEncodingException(
            "Insufficient bytes in RLP encoding: expected " + lengthOfLength + " but have only " + remaining);
      }

      remaining -= lengthOfLength;
      int length = getLength(lengthOfLength, lenient, "value");

      if (remaining < length) {
        throw new InvalidRLPEncodingException(
            "Insufficient bytes in RLP encoding: expected " + length + " but have only " + remaining);
      }

      index += 1 + lengthOfLength + length;
      return;
    }
    if (prefix <= 0xf7) {
      int length = prefix - 0xc0;
      if (remaining < length) {
        throw new InvalidRLPEncodingException(
            "Insufficient bytes in RLP encoding: expected " + length + " but have only " + remaining);
      }
      index += 1 + length;
      return;
    }

    int lengthOfLength = prefix - 0xf7;
    if (remaining < lengthOfLength) {
      throw new InvalidRLPEncodingException(
          "Insufficient bytes in RLP encoding: expected " + lengthOfLength + " but have only " + remaining);
    }

    remaining -= lengthOfLength;
    int length = getLength(lengthOfLength, lenient, "list");

    if (remaining < length) {
      throw new InvalidRLPEncodingException(
          "Insufficient bytes in RLP encoding: expected " + lengthOfLength + " but have only " + remaining);
    }

    index += 1 + lengthOfLength + length;
  }

  @Override
  public int remaining() {
    int oldIndex = index;
    try {
      int count = 0;
      while (!isComplete()) {
        count++;
        skipNext();
      }
      return count;
    } finally {
      index = oldIndex;
    }
  }

  @Override
  public boolean isComplete() {
    return (content.size() - index) == 0;
  }

  @Override
  public int position() {
    return index;
  }

  private Bytes readList(boolean lenient) {
    int remaining = content.size() - index;
    if (remaining == 0) {
      throw new EndOfRLPException();
    }
    int prefix = (((int) content.get(index)) & 0xFF);
    if (prefix <= 0xbf) {
      throw new InvalidRLPTypeException("Attempted to read a list but next item is a value");
    }
    remaining--;

    if (prefix <= 0xf7) {
      int length = prefix - 0xc0;
      if (remaining < length) {
        throw new InvalidRLPEncodingException(
            "Insufficient bytes in RLP encoding: expected " + length + " but have only " + remaining);
      }
      index++;
      Bytes bytes = content.slice(index, length);
      index += length;
      return bytes;
    }

    int lengthOfLength = prefix - 0xf7;
    if (remaining < lengthOfLength) {
      throw new InvalidRLPEncodingException(
          "Insufficient bytes in RLP encoding: expected " + lengthOfLength + " but have only " + remaining);
    }

    remaining -= lengthOfLength;
    int length = getLength(lengthOfLength, lenient, "list");

    if (remaining < length) {
      throw new InvalidRLPEncodingException(
          "Insufficient bytes in RLP encoding: expected " + length + " but have only " + remaining);
    }

    index += 1 + lengthOfLength;
    Bytes bytes = content.slice(index, length);
    index += length;
    return bytes;
  }

  private int getLength(int lengthOfLength, boolean lenient, String type) {
    Bytes lengthBytes = content.slice(index + 1, lengthOfLength);
    if (!lenient) {
      if (lengthBytes.hasLeadingZeroByte()) {
        throw new InvalidRLPEncodingException("RLP " + type + " length contains leading zero bytes");
      }
    } else {
      lengthBytes = lengthBytes.trimLeadingZeros();
    }
    if (lengthBytes.size() == 0) {
      throw new InvalidRLPEncodingException("RLP " + type + " length is zero");
    }
    // Check if the length is greater than a 4 byte integer
    if (lengthBytes.size() > 4) {
      throw new InvalidRLPEncodingException("RLP " + type + " length is oversized");
    }
    int length = lengthBytes.toInt();
    if (length < 0) {
      // Java ints are two's compliment, so this was oversized
      throw new InvalidRLPEncodingException("RLP " + type + " length is oversized");
    }
    assert length > 0;
    if (!lenient && length <= 55) {
      throw new InvalidRLPEncodingException("RLP " + type + " length of " + length + " was not minimally encoded");
    }
    return length;
  }
}
