// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p.proxy

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.net.NetClient
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import org.apache.tuweni.bytes.Bytes
import org.apache.tuweni.concurrent.AsyncResult
import org.apache.tuweni.concurrent.coroutines.await
import kotlin.coroutines.CoroutineContext

class TcpUpstream(
  val vertx: Vertx,
  val host: String,
  val port: Int,
  override val coroutineContext: CoroutineContext = Dispatchers.Default
) : CoroutineScope,
  ClientHandler {

  var tcpclient: NetClient? = null

  fun start() = async {
    tcpclient = vertx.createNetClient()
  }

  fun close() = async {
    tcpclient?.close()
  }

  override suspend fun handleRequest(message: Bytes): Bytes {
    val socket = tcpclient!!.connect(port, host).await()

    val result = AsyncResult.incomplete<Bytes>()
    socket.handler {
      result.complete(Bytes.wrapBuffer(it))
      socket.close()
    }.closeHandler {
      result.complete(Bytes.EMPTY)
    }.exceptionHandler {
      result.completeExceptionally(it)
    }
    socket.write(Buffer.buffer(message.toArrayUnsafe())).await()

    return result.await()
  }
}
