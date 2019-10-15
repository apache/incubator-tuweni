package org.apache.tuweni.devp2p.v5

import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.v5.packet.UdpMessage

interface MessageDecoder<T: UdpMessage> {

  fun create(content: Bytes, src: Bytes, nodeId: Bytes): T

}
