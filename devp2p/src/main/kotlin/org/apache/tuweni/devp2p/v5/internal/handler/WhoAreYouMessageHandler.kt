package org.apache.tuweni.devp2p.v5.internal.handler

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import java.net.InetSocketAddress

class WhoAreYouMessageHandler: MessageHandler<WhoAreYouMessage> {

  override fun handle(message: WhoAreYouMessage, address: InetSocketAddress, connector: UdpConnector) {
    println("Recieved WHOAREYOU message with auth tag ${message.authTag} and nonce ${message.idNonce}")
  }

}
