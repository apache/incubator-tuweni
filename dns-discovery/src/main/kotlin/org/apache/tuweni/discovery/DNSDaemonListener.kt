// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.discovery

import org.apache.tuweni.devp2p.EthereumNodeRecord

/**
 * Callback listening to updates of the DNS records.
 */
@FunctionalInterface
interface DNSDaemonListener {

  /**
   * Callback called when the seq is updated on the DNS server
   * @param seq the update identifier of the records
   * @param records the records stored on the server
   */
  fun newRecords(seq: Long, records: List<EthereumNodeRecord>)
}
