// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.discovery

import org.apache.tuweni.devp2p.EthereumNodeRecord

/**
 * Reads ENR (Ethereum Node Records) entries passed in from DNS.
 *
 * The visitor may decide to stop the visit by returning false.
 */
interface DNSVisitor {

  /**
   * Visit a new ENR record.
   * @param enr the ENR record read from DNS
   * @return true to continue visiting, false otherwise
   */
  fun visit(enr: EthereumNodeRecord): Boolean
}
