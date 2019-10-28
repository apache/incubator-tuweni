package org.apache.tuweni.devp2p.v5.internal.handler

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.packet.NodesMessage
import java.net.InetSocketAddress

class NodesMessageHandler: MessageHandler<NodesMessage> {

  override fun handle(message: NodesMessage, address: InetSocketAddress, srcNodeId: Bytes, connector: UdpConnector) {
    message.nodeRecords.forEach {
      EthereumNodeRecord.fromRLP(it)
      connector.getNodesTable().add(it)
    }
  }

}
