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
