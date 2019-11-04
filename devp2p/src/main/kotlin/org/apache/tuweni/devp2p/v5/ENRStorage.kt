package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.bytes.Bytes

interface ENRStorage {

  fun set(enr: Bytes)

  fun find(nodeId: Bytes): Bytes?

}
