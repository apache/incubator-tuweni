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
package org.apache.tuweni.scuttlebutt.rpc;

/**
 * Defines constants for dealing with SecureScuttlebutt RPC flags.
 */
public interface RPCFlag {

  /**
   * The value of the flag
   * 
   * @return the value of the flag to set on a byte.
   */
  int value();

  /**
   * Applies the flag to the byte
   * 
   * @param flagsByte the byte to apply the bit to
   * @return the modified byte
   */
  default byte apply(byte flagsByte) {
    return (byte) (flagsByte | value());
  }

  /**
   * Checks if the flag bit is applied to this byte
   * 
   * @param flagsByte the flag byte
   * @return true if the flag is set
   */
  default boolean isApplied(byte flagsByte) {
    return (flagsByte & value()) == value();
  }

  /**
   * Flag to set a stream message.
   */
  enum Stream implements RPCFlag {
    /**
     * Stream flag
     */
    STREAM(1 << 3);

    private final int value;

    Stream(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }
  }

  /**
   * Flag to set an end or error message.
   */
  enum EndOrError implements RPCFlag {
    /**
     * End flag
     */
    END(1 << 2);

    private final int value;

    EndOrError(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }
  }

  /**
   * Flag to set a RPC body type.
   */
  enum BodyType implements RPCFlag {
    /**
     * Binary content
     */
    BINARY(0),
    /**
     * String content
     */
    UTF_8_STRING(1),
    /**
     * JSON content
     */
    JSON(1 << 1);

    private final int value;

    BodyType(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }

    @Override
    public boolean isApplied(byte flagByte) {
      if (flagByte == BINARY.value) {
        return this == BINARY;
      }
      return (flagByte & (byte) value) != 0;
    }

    /**
     * Extract the body type from a flag byte
     * 
     * @param flagByte the flag byte encoding the body type
     * @return the body type, either JSON, UTF_8_STRING or BINARY
     */
    public static BodyType extractBodyType(byte flagByte) {
      if (BINARY.isApplied(flagByte)) {
        return BINARY;
      }
      if (UTF_8_STRING.isApplied(flagByte)) {
        return UTF_8_STRING;
      }
      return JSON;
    }
  }
}
