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
package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.misc.DecodeResult
import org.apache.tuweni.devp2p.v5.misc.EncodeResult
import org.apache.tuweni.devp2p.v5.misc.HandshakeInitParameters

/**
 * Message reader/writer. It encodes and decodes messages, structured like at schema below
 *
 * tag || auth_tag || message
 *
 * tag || auth_header || message
 *
 * magic || message
 *
 * It also responsible for encryption functionality, so handlers receives raw messages for processing
 */
internal interface PacketCodec {

  /**
   * Encodes message, encrypting its body
   *
   * @param message message for encoding
   * @param destNodeId receiver node identifier for tag creation
   * @param handshakeParams optional handshake parameter, if it is required to initialize handshake
   *
   * @return encoded message
   */
  fun encode(message: UdpMessage, destNodeId: Bytes, handshakeParams: HandshakeInitParameters? = null): EncodeResult

  /**
   * Decodes message, decrypting its body
   *
   * @param message message for decoding
   *
   * @return decoding result, including sender identifier and decoded message
   */
  fun decode(message: Bytes): DecodeResult
}
