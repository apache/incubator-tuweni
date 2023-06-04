// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.eth

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.coroutines.asyncCompletion
import org.apache.tuweni.rlpx.RLPxService
import org.apache.tuweni.rlpx.wire.DisconnectReason
import org.apache.tuweni.rlpx.wire.SubProtocolHandler
import org.apache.tuweni.rlpx.wire.WireConnection
import org.slf4j.LoggerFactory
import java.util.WeakHashMap
import kotlin.collections.set
import kotlin.coroutines.CoroutineContext

internal class EthHelloHandler(
  override val coroutineContext: CoroutineContext = Dispatchers.Default,
  private val blockchainInfo: BlockchainInformation,
  private val service: RLPxService,
  private val controller: EthHelloController,
) : SubProtocolHandler, CoroutineScope {

  private val pendingStatus = WeakHashMap<String, PeerInfo>()

  companion object {
    val logger = LoggerFactory.getLogger(EthHandler::class.java)!!
  }

  override fun handle(connection: WireConnection, messageType: Int, message: Bytes) = asyncCompletion {
    logger.debug("Receiving message of type {}", messageType)
    when (messageType) {
      MessageType.Status.code -> handleStatus(connection, StatusMessage.read(message))
      else -> {
        service.disconnect(connection, DisconnectReason.CLIENT_QUITTING)
      }
    }
  }

  private suspend fun handleStatus(connection: WireConnection, status: StatusMessage) {
    logger.debug("Received status message {}", status)
    val peerInfo = pendingStatus.remove(connection.uri())
    if (peerInfo == null) {
      peerInfo?.cancel()
      service.disconnect(connection, DisconnectReason.SUBPROTOCOL_REASON)
    } else {
      peerInfo.complete()
      controller.receiveStatus(connection, status.toStatus())
    }
  }

  override fun handleNewPeerConnection(connection: WireConnection): AsyncCompletion {
    val newPeer = PeerInfo()
    pendingStatus[connection.uri()] = newPeer
    val ethSubProtocol = connection.agreedSubprotocols().firstOrNull() { it.name() == EthSubprotocol.ETH66.name() }
    if (ethSubProtocol == null) {
      newPeer.cancel()
      return newPeer.ready
    }
    val forkId =
      blockchainInfo.getLastestApplicableFork(0L)
    service.send(
      ethSubProtocol,
      MessageType.Status.code,
      connection,
      StatusMessage(
        ethSubProtocol.version(),
        blockchainInfo.networkID(),
        blockchainInfo.totalDifficulty(),
        blockchainInfo.bestHash(),
        blockchainInfo.genesisHash(),
        forkId.hash,
        forkId.next,
      ).toBytes(),
    )

    return newPeer.ready
  }

  override fun stop() = asyncCompletion {
  }
}
