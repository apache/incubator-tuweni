/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.rpc

import kotlin.experimental.and

/**
 * Defines constants for dealing with SecureScuttlebutt RPC flags.
 */
interface RPCFlag {
  /**
   * The value of the flag
   *
   * @return the value of the flag to set on a byte.
   */
  fun value(): Int

  /**
   * Applies the flag to the byte
   *
   * @param flagsByte the byte to apply the bit to
   * @return the modified byte
   */
  fun apply(flagsByte: Byte): Byte {
    return (flagsByte.toInt() or value()).toByte()
  }

  /**
   * Checks if the flag bit is applied to this byte
   *
   * @param flagsByte the flag byte
   * @return true if the flag is set
   */
  fun isApplied(flagsByte: Byte): Boolean {
    return flagsByte.toInt() and value() == value()
  }

  /**
   * Flag to set a stream message.
   */
  enum class Stream(private val value: Int) : RPCFlag {
    /**
     * Stream flag
     */
    STREAM(1 shl 3);

    override fun value(): Int {
      return value
    }
  }

  /**
   * Flag to set an end or error message.
   */
  enum class EndOrError(private val value: Int) : RPCFlag {
    /**
     * End flag
     */
    END(1 shl 2);

    override fun value(): Int {
      return value
    }
  }

  /**
   * Flag to set a RPC body type.
   */
  enum class BodyType(private val value: Int) : RPCFlag {
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
    JSON(1 shl 1);

    override fun value(): Int {
      return value
    }

    override fun isApplied(flagsByte: Byte): Boolean {
      if (flagsByte.toInt() == 0) {
        return this == BINARY
      }
      return flagsByte.and(value.toByte()).toInt() != 0
    }

    companion object {
      /**
       * Extract the body type from a flag byte
       *
       * @param flagByte the flag byte encoding the body type
       * @return the body type, either JSON, UTF_8_STRING or BINARY
       */
      fun extractBodyType(flagByte: Byte): BodyType {
        if (BINARY.isApplied(flagByte)) {
          return BINARY
        }
        return if (UTF_8_STRING.isApplied(flagByte)) {
          UTF_8_STRING
        } else {
          JSON
        }
      }
    }
  }
}
