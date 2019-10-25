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
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import java.net.InetSocketAddress

/**
 * Udp message handler, aimed to process it's parameters and sending result
 */
interface MessageHandler<T : UdpMessage> {

  /**
   * @param message udp message containing parameters
   * @param address sender address
   * @param srcNodeId sender node identifier
   * @param connector connector for response send if required
   */
  fun handle(message: T, address: InetSocketAddress, srcNodeId: Bytes, connector: UdpConnector)
}
