package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import java.net.InetSocketAddress

interface MessageHandler<T: UdpMessage> {

  fun handle(message: T, address: InetSocketAddress, connector: UdpConnector)

}
