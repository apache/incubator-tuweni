package org.apache.tuweni.devp2p.v5.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.crypto.Hash
import org.apache.tuweni.crypto.SECP256K1
import org.apache.tuweni.devp2p.EthereumNodeRecord
import org.apache.tuweni.devp2p.parseEnodeUri
import org.apache.tuweni.devp2p.v5.NodeDiscoveryService
import org.apache.tuweni.devp2p.v5.UdpConnector
import org.apache.tuweni.devp2p.v5.packet.RandomMessage
import org.apache.tuweni.io.Base64URLSafe
import java.net.InetSocketAddress
import java.time.Instant
import kotlin.coroutines.CoroutineContext

class DefaultNodeDiscoveryService(
  private val keyPair: SECP256K1.KeyPair,
  private val localPort: Int,
  private val bindAddress: InetSocketAddress = InetSocketAddress(localPort),
  private val bootstrapENRList: List<String> = emptyList(),
  private val enrSeq: Long = Instant.now().toEpochMilli(),
  private val selfENR: Bytes = EthereumNodeRecord.toRLP(keyPair, enrSeq, emptyMap(), bindAddress.address, null, bindAddress.port),
  private val nodeId: Bytes = Hash.sha2_256(selfENR),
  private val connector: UdpConnector = DefaultUdpConnector(nodeId, bindAddress, keyPair),
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : NodeDiscoveryService, CoroutineScope {

  override fun start() {
    connector.start()
    launch { bootstrap() }
  }

  override fun terminate() {
    connector.terminate()
  }

  private fun bootstrap() {
    bootstrapENRList.map {
      val encodedEnr = it.substringAfter("enr:")
      val rlpENR = Base64URLSafe.decode(encodedEnr)
      val enr = EthereumNodeRecord.fromRLP(rlpENR)
      val destNodeId = Hash.sha2_256(rlpENR)

      val randomMessage = RandomMessage(nodeId, destNodeId)
      val address = InetSocketAddress(enr.ip(), enr.udp())

      connector.addPendingNodeId(address, rlpENR)
      connector.send(address, randomMessage)
    }
  }

}
