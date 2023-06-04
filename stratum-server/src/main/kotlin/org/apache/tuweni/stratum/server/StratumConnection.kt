// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.stratum.server

import io.vertx.core.buffer.Buffer
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

/**
 * Persistent TCP connection using a variant of the Stratum protocol, connecting the client to
 * miners.
 */
class StratumConnection(
  private val protocols: Array<StratumProtocol>,
  val closeHandle: (Boolean) -> Unit,
  val sender: (String) -> Unit,
  val name: String,
  val threshold: Int = 3
) {

  companion object {
    private val logger = LoggerFactory.getLogger(StratumConnection::class.java)
  }
  private var incompleteMessage = ""
  private var protocol: StratumProtocol? = null
  private var errors = 0

  fun handleBuffer(buffer: Buffer) {
    logger.trace("Buffer received {}", buffer)
    var firstMessage = false
    val messagesString: String
    messagesString = try {
      buffer.toString(StandardCharsets.UTF_8)
    } catch (e: IllegalArgumentException) {
      logger.debug("Invalid message with non UTF-8 characters: ${e.message}", e)
      closeHandle(true)
      return
    }
    val messages: Iterator<String> = messagesString.split('\n').iterator()
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

  fun close(addToDenyList: Boolean) {
    logger.trace("Closing connection")
    protocol?.onClose(this)
    closeHandle(addToDenyList)
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
        closeHandle(true)
      }
    } else {
      protocol?.handle(this, message)
    }
  }

  fun send(message: String) {
    logger.debug("Sending message {}", message)
    sender(message)
  }

  fun handleClientResponseFeedback(result: Boolean) {
    if (result) {
      errors = 0
    } else {
      errors += 1
      if (errors > threshold) {
        logger.warn("Too many errors with handle $name, closing")
        close(true)
      }
    }
  }
}
