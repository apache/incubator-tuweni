/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.rlpx.wire;


import net.consensys.cava.rlpx.RLPxService;

/**
 * Defines a subprotocol to be used for wire connections
 */
public interface SubProtocol {

  /**
   * @return the identifier of the subprotocol
   */
  SubProtocolIdentifier id();

  /**
   * @param subProtocolIdentifier the identifier of the subprotocol
   * @return true if the subprotocol ID and version are supported, false otherwise
   */
  boolean supports(SubProtocolIdentifier subProtocolIdentifier);

  /**
   * Provides the length of the range of message types supported by the subprotocol for a given version
   *
   * @param version the version of the subprotocol to associate with the range
   * @return the length of the range of message types supported by the subprotocol for a given version
   */
  int versionRange(int version);

  /**
   * Creates a new handler for the subprotocol.
   *
   * @param service the rlpx service that will use the handler
   * @return a new handler for the subprotocol, bound to the service.
   */
  SubProtocolHandler createHandler(RLPxService service);
}
