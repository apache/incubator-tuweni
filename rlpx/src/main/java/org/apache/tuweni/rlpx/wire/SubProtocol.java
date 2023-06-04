// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
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
