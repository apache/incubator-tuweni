package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import java.nio.ByteBuffer

interface PacketCodec {

  fun encode(message: UdpMessage): ByteBuffer

  fun decode(buffer: ByteBuffer): UdpMessage

}
