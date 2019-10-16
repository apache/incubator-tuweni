package org.apache.tuweni.devp2p.v5

import kotlinx.coroutines.Job
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.v5.packet.UdpMessage
import java.net.InetSocketAddress

interface UdpConnector {

  fun start(): Job

  fun terminate()

  fun send(address: InetSocketAddress, message: UdpMessage, encryptionKey: Bytes = Bytes.EMPTY, encryptionNonce: Bytes = Bytes.EMPTY)

  fun available(): Boolean

  fun started(): Boolean

  fun addPendingNodeId(address: InetSocketAddress, nodeId: Bytes)

  fun getPendingNodeIdByAddress(address: InetSocketAddress): Bytes

  fun getNodeKeyPair(): SECP256K1.KeyPair

}
