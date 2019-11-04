package org.apache.tuweni.devp2p.v5.misc

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.devp2p.v5.packet.UdpMessage

class TrackingMessage(
  val message: UdpMessage,
  val nodeId: Bytes
)
