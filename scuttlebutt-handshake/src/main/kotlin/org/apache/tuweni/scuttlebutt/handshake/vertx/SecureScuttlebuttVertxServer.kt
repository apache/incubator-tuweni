/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tuweni.scuttlebutt.handshake.vertx

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetServer
import io.vertx.core.net.NetServerOptions
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.await
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.scuttlebutt.handshake.HandshakeException
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttHandshakeServer.Companion.create
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttStreamServer
import org.apache.tuweni.scuttlebutt.handshake.StreamException
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress

/**
 * Secure Scuttlebutt server using Vert.x to manage persistent TCP connections.
 *
 * @param vertx the Vert.x instance
 * @param addr the network interface and port to bind the server to
 * @param keyPair the identity of the server according to the Secure Scuttlebutt protocol
 * @param networkIdentifier the network identifier of the server according to the Secure Scuttlebutt protocol
 * @param handlerFactory the factory of handlers that will manage stream connections
 */
class SecureScuttlebuttVertxServer(
  private val vertx: Vertx,
  private val addr: InetSocketAddress,
  private val keyPair: Signature.KeyPair,
  private val networkIdentifier: Bytes32,
  private val handlerFactory: (writer: (Bytes) -> Unit, terminationFn: () -> Unit) -> ServerHandler
) {

  val port: Int
    get() = if (server == null) {
      0
    } else {
      server!!.actualPort()
    }

  companion object {
    private val logger = LoggerFactory.getLogger(SecureScuttlebuttVertxServer::class.java)
  }

  private inner class NetSocketHandler {
    var handshakeCounter = 0
    var netSocket: NetSocket? = null
    var handler: ServerHandler? = null
    var streamServer: SecureScuttlebuttStreamServer? = null
    var handshakeServer = create(
      keyPair,
      networkIdentifier
    )
    private var messageBuffer = Bytes.EMPTY

    fun handle(netSocket: NetSocket) {
      this.netSocket = netSocket
      netSocket.closeHandler { _ ->
        if (handler != null) {
          handler!!.streamClosed()
        }
      }
      netSocket.exceptionHandler { e: Throwable ->
        logger.error(
          e.message,
          e
        )
      }
      netSocket.handler { buffer: Buffer ->
        handleMessage(
          buffer
        )
      }
    }

    private fun handleMessage(buffer: Buffer) {
      try {
        if (handshakeCounter == 0) {
          handshakeServer.readHello(Bytes.wrapBuffer(buffer))
          netSocket!!.write(Buffer.buffer(handshakeServer.createHello().toArrayUnsafe()))
          handshakeCounter++
        } else if (handshakeCounter == 1) {
          handshakeServer.readIdentityMessage(Bytes.wrapBuffer(buffer))
          netSocket!!.write(Buffer.buffer(handshakeServer.createAcceptMessage().toArrayUnsafe()))
          streamServer = handshakeServer.createStream()
          handshakeCounter++
          handler = handlerFactory(
            { bytes: Bytes? ->
              synchronized(this@NetSocketHandler) {
                netSocket!!.write(
                  Buffer.buffer(
                    streamServer!!.sendToClient(bytes!!).toArrayUnsafe()
                  )
                )
              }
            }
          ) {
            synchronized(this@NetSocketHandler) {
              netSocket!!.write(
                Buffer.buffer(
                  streamServer!!.sendGoodbyeToClient().toArrayUnsafe()
                )
              )
              netSocket!!.close()
            }
          }
        } else {
          val message = streamServer!!.readFromClient(Bytes.wrapBuffer(buffer))
          messageBuffer = Bytes.concatenate(messageBuffer, message)
          val headerSize = 9

          // Process any whole RPC message responses we have, and leave any partial ones at the end in the buffer
          // We may have 1 or more whole messages, or 1 and a half, etc..
          while (messageBuffer.size() >= headerSize) {
            val header = messageBuffer.slice(0, 9)
            val bodyLength = getBodyLength(header)
            messageBuffer = if (messageBuffer.size() - headerSize >= bodyLength) {
              val headerAndBodyLength = bodyLength + headerSize
              val wholeMessage = messageBuffer.slice(0, headerAndBodyLength)
              if (SecureScuttlebuttStreamServer.isGoodbye(wholeMessage)) {
                netSocket!!.close()
              } else {
                handler!!.receivedMessage(wholeMessage.slice(9))
              }

              // We've removed 1 RPC message from the message buffer, leave the remaining messages / part of a message
              // in the buffer to be processed in the next iteration
              messageBuffer.slice(headerAndBodyLength)
            } else {
              // We don't have a full RPC message, leave the bytes in the buffer for when more arrive
              break
            }
          }
        }
      } catch (e: HandshakeException) {
        netSocket!!.close()
        logger.error(e.message, e)
        throw e
      } catch (e: StreamException) {
        netSocket!!.close()
        logger.error(e.message, e)
        throw e
      }
    }
  }

  private fun getBodyLength(rpcHeader: Bytes): Int {
    val size = rpcHeader.slice(1, 4)
    return size.toInt()
  }

  private var server: NetServer? = null

  /**
   * Starts the server.
   *
   * @return a handle to the completion of the operation
   */
  suspend fun start() {
    server = vertx
      .createNetServer(
        NetServerOptions().setTcpKeepAlive(true).setHost(addr.hostString).setPort(addr.port)
      )
    server!!.connectHandler { netSocket: NetSocket ->
      NetSocketHandler().handle(netSocket)
    }
    server!!.listen().await()
  }

  /**
   * Stops the server.
   *
   * @return a handle to the completion of the operation
   */
  suspend fun stop() {
    server!!.close().await()
  }
}
