package org.apache.tuweni.devp2p.v5.dht

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.kademlia.KademliaRoutingTable
import org.apache.tuweni.kademlia.xorDist

class RoutingTable(
  selfEnr: Bytes
) {

  private val selfNodeId = key(selfEnr)
  private val nodeIdCalculation: (Bytes) -> ByteArray = { enr -> key(enr) }
  private val table = KademliaRoutingTable(
    selfId = selfNodeId,
    k = BUCKET_SIZE,
    nodeId = nodeIdCalculation,
    distanceToSelf = { key(it) xorDist selfNodeId })


  fun add(enr: Bytes): Bytes? = table.add(enr)

  fun nearest(nodeId: Bytes, limit: Int): List<Bytes> = table.nearest(nodeId.toArray(), limit)

  fun evict(enr: Bytes): Boolean = table.evict(enr)

  fun nodesOfDistance(distance: Int): List<Bytes> = table.peersOfDistance(distance)

  private fun key(enr: Bytes): ByteArray = Hash.sha2_256(enr).toArray()

  companion object {
    private const val BUCKET_SIZE: Int = 16
  }

}
