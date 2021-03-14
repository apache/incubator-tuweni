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
package org.apache.tuweni.stratum.server

import com.google.common.base.Splitter
import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

/**
 * Persistent TCP connection using a variant of the Stratum protocol, connecting the client to
 * miners.
 */
class StratumConnection(
  private val protocols: Array<StratumProtocol>,
  val closeHandle: () -> Unit,
  val sender: (String) -> Unit
) {

  companion object {
    private val logger = LoggerFactory.getLogger(StratumConnection::class.java)
  }
  private var incompleteMessage = ""
  private var protocol: StratumProtocol? = null

  fun handleBuffer(buffer: Buffer) {
    logger.trace("Buffer received {}", buffer)
    val splitter: Splitter = Splitter.on('\n')
    var firstMessage = false
    val messagesString: String
    messagesString = try {
      buffer.toString(StandardCharsets.UTF_8)
    } catch (e: IllegalArgumentException) {
      logger.debug("Invalid message with non UTF-8 characters: ${e.message}", e)
      closeHandle()
      return
    }
    val messages: Iterator<String> = splitter.split(messagesString).iterator()
    while (messages.hasNext()) {
      var message = messages.next()
      if (!firstMessage) {
        message = incompleteMessage + message
        firstMessage = true
      }
      if (!messages.hasNext()) {
        incompleteMessage = message
      } else {
        logger.trace("Dispatching message {}", message)
        handleMessage(message)
      }
    }
  }

  fun close() {
    logger.trace("Closing connection")
    protocol?.onClose(this)
  }

  private fun handleMessage(message: String) {
    if (protocol == null) {
      for (protocol in protocols) {
        if (protocol.canHandle(message, this)) {
          this.protocol = protocol
        }
      }
      if (protocol == null) {
        logger.debug("Invalid first message: {}", message)
        closeHandle()
      }
    } else {
      protocol?.handle(this, message)
    }
  }

  fun send(message: String) {
    logger.debug("Sending message {}", message)
    sender(message)
  }
}
