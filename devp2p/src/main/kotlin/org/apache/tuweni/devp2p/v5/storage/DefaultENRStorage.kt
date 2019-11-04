package org.apache.tuweni.devp2p.v5.storage

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.devp2p.v5.ENRStorage
import java.util.concurrent.ConcurrentHashMap

class DefaultENRStorage: ENRStorage {

  private val storage: MutableMap<String, Bytes> = ConcurrentHashMap()

  override fun find(nodeId: Bytes): Bytes? = storage[nodeId.toHexString()]

  override fun set(enr: Bytes) {
    val nodeId = Hash.sha2_256(enr)
    storage[nodeId.toHexString()] = enr
  }

}
