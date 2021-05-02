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
package org.apache.tuweni.devp2p.proxy

import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.SubProtocol
import org.apache.tuweni.rlpx.wire.SubProtocolClient
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier

class ProxySubprotocol : SubProtocol {

  companion object {
    val ID = SubProtocolIdentifier.of("pxy", 1, 3)
  }

  override fun id() = ID

  override fun supports(subProtocolIdentifier: SubProtocolIdentifier): Boolean = subProtocolIdentifier == ID

  override fun createHandler(service: RLPxService, client: SubProtocolClient) =
    ProxyHandler(service = service, client = client as ProxyClient)

  override fun createClient(service: RLPxService, subProtocolIdentifier: SubProtocolIdentifier) = ProxyClient(service)
}
