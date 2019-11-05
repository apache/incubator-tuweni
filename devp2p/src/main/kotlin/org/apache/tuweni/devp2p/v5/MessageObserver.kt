package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.devp2p.v5.packet.UdpMessage

interface MessageObserver {

  fun observe(message: UdpMessage)

}
