package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import java.nio.ByteBuffer

interface PacketCodec {

  fun decode(buffer: ByteBuffer): UdpMessage

}
