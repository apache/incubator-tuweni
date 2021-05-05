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


import org.apache.tuweni.rlpx.RLPxService;

import java.util.Collections;
import java.util.List;

/**
 * Defines a subprotocol to be used for wire connections
 */
public interface SubProtocol {

  /**
   * Provides the identifier of the subprotocol
   * 
   * @return the identifier of the subprotocol
   */
  SubProtocolIdentifier id();

  /**
   * Returns true if the subprotocol supports a sub protocol ID.
   * 
   * @param subProtocolIdentifier the identifier of the subprotocol
   * @return true if the subprotocol ID and version are supported, false otherwise
   */
  boolean supports(SubProtocolIdentifier subProtocolIdentifier);

  /**
   * Creates a new handler for the subprotocol.
   *
   * @param service the rlpx service that will use the handler
   * @param client the subprotocol client
   * @return a new handler for the subprotocol, bound to the service.
   */
  SubProtocolHandler createHandler(RLPxService service, SubProtocolClient client);

  /**
   * Creates a new client for the subprotocol.
   *
   * @param service the rlpx service that will use the handler
   * @param identifier the version of the subprotocol
   * @return a new client for the subprotocol, bound to the service.
   */
  SubProtocolClient createClient(RLPxService service, SubProtocolIdentifier identifier);

  /**
   * Provides the capabilities supported by the subprotocol.
   * 
   * @return the capabilities for this protocol, ordered.
   */
  default List<SubProtocolIdentifier> getCapabilities() {
    return Collections.singletonList(id());
  }
}
