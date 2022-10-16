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
import io.vertx.core.net.NetClient
import io.vertx.core.net.NetClientOptions
import io.vertx.core.net.NetSocket
import io.vertx.kotlin.coroutines.await
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.bytes.Bytes32
import org.apache.tuweni.concurrent.AsyncCompletion
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.await
import org.apache.tuweni.crypto.sodium.Signature
import org.apache.tuweni.scuttlebutt.Invite
import org.apache.tuweni.scuttlebutt.handshake.HandshakeException
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttHandshakeClient
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttHandshakeClient.Companion.create
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttHandshakeClient.Companion.fromInvite
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttStreamClient
import org.apache.tuweni.scuttlebutt.handshake.SecureScuttlebuttStreamServer
import org.apache.tuweni.scuttlebutt.handshake.StreamException
import org.slf4j.LoggerFactory

/**
 * Secure Scuttlebutt client using Vert.x to manage persistent TCP connections.
 *
 * @param vertx the Vert.x instance
 * @param keyPair the identity of the server according to the Secure Scuttlebutt protocol
 * @param networkIdentifier the network identifier of the server according to the Secure Scuttlebutt protocol
 */
class SecureScuttlebuttVertxClient(private val vertx: Vertx, private val keyPair: Signature.KeyPair, private val networkIdentifier: Bytes32) {
  private inner class NetSocketClientHandler(
    private val socket: NetSocket,
    remotePublicKey: Signature.PublicKey?,
    invite: Invite?,
    private val handlerFactory: (sender: (Bytes) -> Unit, terminationFunction: () -> Unit) -> ClientHandler
  ) {
    private val handshakeClient: SecureScuttlebuttHandshakeClient
    private var handshakeCounter = 0
    private var client: SecureScuttlebuttStreamClient? = null
    private var handler: ClientHandler? = null
    private var messageBuffer = Bytes.EMPTY

    val result = AsyncResult.incomplete<ClientHandler>()

    init {
      handshakeClient = if (invite != null) {
        fromInvite(networkIdentifier, invite)
      } else {
        create(
          keyPair,
          networkIdentifier,
          remotePublicKey
        )
      }
      socket.closeHandler {
        if (handler != null) {
          handler!!.streamClosed()
        }
        if (!result.isDone) {
          result.completeExceptionally(IllegalStateException("Connection closed before handshake"))
        }
      }
      socket.exceptionHandler { e: Throwable ->
        logger.error(
          e.message,
          e
        )
      }
      socket.handler { buffer: Buffer? ->
        handle(
          buffer
        )
      }
      socket.write(Buffer.buffer(handshakeClient.createHello().toArrayUnsafe()))
    }

    fun handle(buffer: Buffer?) {
      try {
        if (handshakeCounter == 0) {
          handshakeClient.readHello(Bytes.wrapBuffer(buffer!!))
          socket.write(Buffer.buffer(handshakeClient.createIdentityMessage().toArrayUnsafe()))
          handshakeCounter++
        } else if (handshakeCounter == 1) {
          handshakeClient.readAcceptMessage(Bytes.wrapBuffer(buffer!!))
          client = handshakeClient.createStream()
          handler = handlerFactory(
            { bytes: Bytes? ->
              synchronized(this@NetSocketClientHandler) {
                socket.write(
                  Buffer.buffer(
                    client!!.sendToServer(bytes!!).toArrayUnsafe()
                  )
                )
              }
            }
          ) {
            synchronized(this@NetSocketClientHandler) {
              socket.write(
                Buffer.buffer(
                  client!!.sendGoodbyeToServer().toArrayUnsafe()
                )
              )
              socket.close()
            }
          }
          handshakeCounter++
          result.complete(handler)
        } else {
          val message = client!!.readFromServer(
            Bytes.wrapBuffer(
              buffer!!
            )
          )
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
                logger.debug("Goodbye received from remote peer")
                socket.close()
              } else {
                handler!!.receivedMessage(wholeMessage)
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
        result.completeExceptionally(e)
        logger.debug(e.message, e)
        socket.close()
      } catch (e: StreamException) {
        result.completeExceptionally(e)
        logger.debug(e.message, e)
        socket.close()
      } catch (t: Throwable) {
        if (!result.isDone) {
          result.completeExceptionally(t)
        }
        logger.error(t.message, t)
        throw RuntimeException(t)
      }
    }
  }

  private fun getBodyLength(rpcHeader: Bytes): Int {
    val size = rpcHeader.slice(1, 4)
    return size.toInt()
  }

  private var client: NetClient? = null

  /**
   * Connects the client to a remote host.
   *
   * @param <T> the type of client handler
   * @param port the port of the remote host
   * @param host the host string of the remote host
   * @param remotePublicKey the public key of the remote host, may be null if an invite is used.
   * @param invite the invite to the server, may be null
   * @param handlerFactory the factory of handlers for connections
   * @return a handle to a new stream handler with the remote host
   </T> */
  suspend fun connectTo(
    port: Int,
    host: String,
    remotePublicKey: Signature.PublicKey?,
    invite: Invite?,
    handlerFactory: (sender: (Bytes) -> Unit, terminationFunction: () -> Unit) -> ClientHandler
  ): ClientHandler {
    client = vertx.createNetClient(NetClientOptions().setTcpKeepAlive(true))
    val socket = client!!.connect(port, host).await()
    val h = NetSocketClientHandler(
      socket,
      remotePublicKey,
      invite,
      handlerFactory
    )
    return h.result.await()
  }

  /**
   * Stops the server.
   *
   * @return a handle to the completion of the operation
   */
  fun stop(): AsyncCompletion {
    client!!.close()
    return AsyncCompletion.completed()
  }

  companion object {
    private val logger = LoggerFactory.getLogger(NetSocketClientHandler::class.java)
  }
}
