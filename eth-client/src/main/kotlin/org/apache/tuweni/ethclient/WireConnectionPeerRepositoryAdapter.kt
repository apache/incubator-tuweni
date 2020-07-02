package org.apache.tuweni.ethclient

import org.apache.tuweni.peer.repository.PeerRepository
import org.apache.tuweni.rlpx.WireConnectionRepository
import org.apache.tuweni.rlpx.wire.SubProtocolIdentifier
import org.apache.tuweni.rlpx.wire.WireConnection

class WireConnectionPeerRepositoryAdapter(peerRepository: PeerRepository) : WireConnectionRepository {
  override fun add(wireConnection: WireConnection) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun get(id: String): WireConnection {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun asIterable(): MutableIterable<WireConnection> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun asIterable(identifier: SubProtocolIdentifier): MutableIterable<WireConnection> {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun close() {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

}
