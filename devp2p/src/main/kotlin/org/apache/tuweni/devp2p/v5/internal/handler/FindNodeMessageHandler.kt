package org.apache.tuweni.devp2p.v5.internal.handler

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.packet.FindNodeMessage
import org.apache.tuweni.devp2p.v5.packet.NodesMessage
import java.net.InetSocketAddress

class FindNodeMessageHandler: MessageHandler<FindNodeMessage> {

  override fun handle(message: FindNodeMessage, address: InetSocketAddress, srcNodeId: Bytes, connector: UdpConnector) {
    val nodes = connector.getNodesTable().nodesOfDistance(message.distance)

    var caret = 0
    while (caret < nodes.size) {
      val response = NodesMessage(message.requestId, nodes.size, nodes.subList(caret, caret + MAX_NODES_IN_RESPONSE))
      connector.send(address, response, srcNodeId)
      caret += MAX_NODES_IN_RESPONSE
    }
  }

  companion object {
      private const val MAX_NODES_IN_RESPONSE: Int = 16
  }

}
