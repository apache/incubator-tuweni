package org.apache.tuweni.devp2p.v5.internal.handler

import org.apache.tuweni.devp2p.v5.MessageHandler
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.devp2p.v5.packet.WhoAreYouMessage
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import java.net.InetSocketAddress

class RandomMessageHandler: MessageHandler<RandomMessage> {

  override fun handle(message: RandomMessage, address: InetSocketAddress, connector: UdpConnector) {
    val response = WhoAreYouMessage(message.dest, message.src)
    connector.send(address, response)
  }

}
