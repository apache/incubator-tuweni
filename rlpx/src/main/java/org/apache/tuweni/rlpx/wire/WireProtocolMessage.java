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
package org.apache.tuweni.rlpx.wire;

import org.apache.tuweni.bytes.Bytes;

/**
 * A set of bytes made available to a subprotocol after it has been successfully decrypted.
 */
interface WireProtocolMessage {


  /**
   * Provides the message payload
   * 
   * @return the payload of the wire message, ready for consumption.
   */
  Bytes toBytes();

  /**
   * Provides the message code
   * 
   * @return the code associated with the message type according to the subprotocol.
   */
  int messageType();
}
