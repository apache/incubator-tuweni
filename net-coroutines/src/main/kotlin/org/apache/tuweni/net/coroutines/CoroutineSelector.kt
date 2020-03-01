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
package org.apache.tuweni.net.coroutines

import com.google.common.util.concurrent.ThreadFactoryBuilder
import org.slf4j.LoggerFactory
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.suspendCancellableCoroutine
import java.nio.channels.CancelledKeyException
import java.nio.channels.ClosedChannelException
import java.nio.channels.ClosedSelectorException
import java.nio.channels.SelectableChannel
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resumeWithException

/**
 * A selector for co-routine based channel IO.
 *
 */
sealed class CoroutineSelector {

  companion object {
    internal val DEFAULT_THREAD_FACTORY =
      ThreadFactoryBuilder().setNameFormat("selection-dispatch-%d").setDaemon(true).build()

    /**
     * Open a co-routine selector.
     *
     * @param executor An executor for obtaining a thread to run the selection loop.
     * @param loggerProvider A provider for logger instances.
     * @param selectTimeout The maximum time the selection operation will wait before checking for closed channels.
     * @param idleTimeout The minimum idle time before the selection loop of the selector exits.
     * @return A co-routine selector.
     */
    fun open(
      executor: Executor = Executors.newSingleThreadExecutor(DEFAULT_THREAD_FACTORY),
      selectTimeout: Long = 1000,
      idleTimeout: Long = 10000
    ): CoroutineSelector {
      require(selectTimeout > 0) { "selectTimeout must be larger than zero" }
      require(idleTimeout >= 0) { "idleTimeout must be positive" }
      val idleTasks = idleTimeout / selectTimeout
      require(idleTasks <= Integer.MAX_VALUE) { "idleTimeout is too large" }

      return SingleThreadCoroutineSelector(executor, Selector.open(), selectTimeout, idleTasks.toInt())
    }
  }

  /**
   * Indicates whether the selector is open or not.
   *
   * @return `true` if the selector is open.
   */
  abstract fun isOpen(): Boolean

  /**
   * Wait for a channel to become ready for any of the specified operations.
   *
   * @param channel The channel.
   * @param ops The interest set, as a combination of [SelectionKey.OP_ACCEPT], [SelectionKey.OP_CONNECT],
   *   [SelectionKey.OP_READ] and/or [SelectionKey.OP_WRITE].
   * @throws ClosedSelectorException If the co-routine selector has been closed.
   */
  abstract suspend fun select(channel: SelectableChannel, ops: Int)

  /**
   * Cancel any suspended calls to [select] for the specified channel.
   *
   * @param channel The channel.
   * @param cause An optional cause for the cancellation.
   * @return `true` if any suspensions were cancelled.
   * @throws ClosedSelectorException If the co-routine selector has been closed.
   */
  abstract suspend fun cancelSelections(channel: SelectableChannel, cause: Throwable? = null): Boolean

  /**
   * Force the selection loop, if running, to wake up and process any closed channels.
   *
   * @throws ClosedSelectorException If the co-routine selector has been closed.
   */
  abstract fun wakeup()

  /**
   * Close the co-routine selector.
   */
  abstract fun close()

  /**
   * Close the co-routine selector and wait for all suspensions to be cancelled.
   */
  abstract suspend fun closeNow()
}

internal class SingleThreadCoroutineSelector(
  private val executor: Executor,
  private val selector: Selector,
  private val selectTimeout: Long,
  private val idleTasks: Int
) : CoroutineSelector() {

  companion object {
    private val logger = LoggerFactory.getLogger(CoroutineSelector::class.java)
  }

  private val pendingInterests = Channel<SelectionInterest>(capacity = Channel.UNLIMITED)
  private val pendingCancellations = Channel<SelectionCancellation>(capacity = Channel.UNLIMITED)
  private val pendingCloses = Channel<Continuation<Unit>>(capacity = Channel.UNLIMITED)
  private val outstandingTasks = AtomicInteger(0)
  private val registeredKeys = HashSet<SelectionKey>()

  init {
    require(selector.isOpen) { "Selector is closed" }
    require(selector.keys().isEmpty()) { "Selector already has selection keys" }
  }

  override fun isOpen(): Boolean = selector.isOpen

  override suspend fun select(channel: SelectableChannel, ops: Int) {
    require(!channel.isBlocking) { "AsyncChannel must be set to non blocking" }
    require(ops != 0) { "ops must not be zero" }
    require(ops and channel.validOps().inv() == 0) { "Invalid operations for channel" }
    if (!selector.isOpen) {
      throw ClosedSelectorException()
    }
    suspendCancellableCoroutine { cont: CancellableContinuation<Unit> ->
      try {
        // increment tasks first to keep selection loop running while we add a new pending interest
        val isRunning = incrementTasks()
        pendingInterests.offer(SelectionInterest(cont, channel, ops))
        wakeup(isRunning)
      } catch (e: Throwable) {
        cont.resumeWithException(e)
      }
    }
  }

  override suspend fun cancelSelections(channel: SelectableChannel, cause: Throwable?): Boolean {
    if (!selector.isOpen) {
      throw ClosedSelectorException()
    }
    check(selector.isOpen) { "Selector is closed" }
    return suspendCancellableCoroutine { cont: CancellableContinuation<Boolean> ->
      try {
        // increment tasks first to keep selection loop running while we add a new pending cancellation
        val isRunning = incrementTasks()
        pendingCancellations.offer(SelectionCancellation(channel, cause, cont))
        wakeup(isRunning)
      } catch (e: Throwable) {
        cont.resumeWithException(e)
      }
    }
  }

  override fun wakeup() {
    if (!selector.isOpen) {
      throw ClosedSelectorException()
    }
    selector.wakeup()
  }

  override fun close() {
    selector.close()
  }

  override suspend fun closeNow() {
    selector.close()
    suspendCoroutine { cont: Continuation<Unit> ->
      try {
        pendingCloses.offer(cont)
        if (outstandingTasks.get() == 0) {
          processPendingCloses()
        }
      } catch (e: Throwable) {
        cont.resumeWithException(e)
        throw e
      }
    }
  }

  private fun incrementTasks(): Boolean = outstandingTasks.getAndIncrement() != 0

  private fun wakeup(isRunning: Boolean) {
    if (isRunning) {
      logger.debug("Selector {}: Interrupting selection loop", System.identityHashCode(selector))
      selector.wakeup()
    } else {
      executor.execute(this::selectionLoop)
    }
  }

  private fun selectionLoop() {
    logger.debug("Selector {}: Starting selection loop", System.identityHashCode(selector))
    try {
      // allow the selector to cleanup any outstanding cancelled keys before starting the loop
      selector.selectNow()
      outstandingTasks.addAndGet(idleTasks)
      var idleCount = 0

      while (true) {
        // add pending selections before awakening selected, which allows for newly added
        // selections to be awoken immediately and avoids trying to register to already canceled keys
        if (processTasks(this::registerPendingSelections)) {
          break
        }
        if (processTasks(this::processPendingCancellations)) {
          break
        }
        if (processTasks(this::awakenSelected)) {
          break
        }

        if (selector.keys().isEmpty()) {
          if (outstandingTasks.decrementAndGet() == 0) {
            break
          }
          idleCount++
        } else {
          outstandingTasks.addAndGet(idleCount)
          idleCount = 0
        }

        selector.selectedKeys().clear()
        // use a timeout on select, as keys cancelled via channel close wont wakeup the selector
        selector.select(selectTimeout)

        if (!selector.isOpen) {
          cancelAll(ClosedSelectorException())
          break
        }
        if (processTasks(this::cancelMissingRegistrations)) {
          break
        }
      }
      logger.debug("Selector {}: Exiting selection loop", System.identityHashCode(selector))
      processPendingCloses()
    } catch (e: Throwable) {
      selector.close()
      logger.error("Selector System.identityHashCode(selector)): An unexpected exception occurred in selection loop", e)
      cancelAll(e)
      processPendingCloses(e)
    }
  }

  private fun processTasks(block: () -> Int): Boolean {
    val processed = block()
    val remaining = outstandingTasks.addAndGet(-processed)
    check(remaining >= 0) { "More tasks processed than were outstanding" }
    return remaining == 0
  }

  private fun registerPendingSelections(): Int {
    var processed = 0
    while (true) {
      val interest = pendingInterests.poll() ?: break
      try {
        val key = interest.channel.keyFor(selector)
        val registered = if (key == null) {
          registerInterest(interest)
        } else {
          mergeInterest(key, interest)
        }
        if (!registered) {
          processed++
        }
      } catch (e: Throwable) {
        interest.cont.resumeWithException(e)
        throw e
      }
    }
    return processed
  }

  private fun registerInterest(interest: SelectionInterest): Boolean {
    val key: SelectionKey
    try {
      key = interest.channel.register(selector, interest.ops, arrayListOf(interest))
    } catch (e: ClosedChannelException) {
      interest.cont.resumeWithException(e)
      return false
    }
    registeredKeys.add(key)
    logger.debug("Selector {}: Registered {}@{} for interests {}", System.identityHashCode(selector),
      interest.channel, System.identityHashCode(interest.channel), interest.ops)
    return true
  }

  private fun mergeInterest(key: SelectionKey, interest: SelectionInterest): Boolean {
    val mergedInterests: Int
    try {
      mergedInterests = key.interestOps() or interest.ops
      key.interestOps(mergedInterests)
    } catch (e: CancelledKeyException) {
      // key must have been cancelled via closing the channel
      val exception = ClosedChannelException()
      exception.addSuppressed(e)
      interest.cont.resumeWithException(exception)
      return false
    }
    @Suppress("UNCHECKED_CAST")
    val interests = key.attachment() as ArrayList<SelectionInterest>
    interests.add(interest)
    logger.debug("Selector {}: Updated registration for channel {} to interests {}",
      System.identityHashCode(selector), System.identityHashCode(interest.channel), mergedInterests)
    return true
  }

  private fun processPendingCancellations(): Int {
    var processed = 0
    while (true) {
      val cancellation = pendingCancellations.poll() ?: break
      processed++
      val key = cancellation.channel.keyFor(selector)
      if (key != null) {
        logger.debug("Selector {}: Cancelling registration for channel {}", System.identityHashCode(selector),
          System.identityHashCode(cancellation.channel))

        @Suppress("UNCHECKED_CAST")
        val interests = key.attachment() as ArrayList<SelectionInterest>
        for (interest in interests) {
          interest.cont.cancel(cancellation.cause)
          processed++
        }
        interests.clear()
        key.cancel()
        registeredKeys.remove(key)
        cancellation.cont.resume(true)
      } else {
        cancellation.cont.resume(false)
      }
    }
    return processed
  }

  private fun processPendingCloses() {
    while (true) {
      (pendingCloses.poll() ?: break).resume(Unit)
    }
  }

  private fun processPendingCloses(e: Throwable) {
    while (true) {
      (pendingCloses.poll() ?: break).resumeWithException(e)
    }
  }

  private fun awakenSelected(): Int {
    var awoken = 0
    val selectedKeys = selector.selectedKeys()
    for (key in selectedKeys) {
      @Suppress("UNCHECKED_CAST")
      val interests = key.attachment() as ArrayList<SelectionInterest>

      if (!key.isValid) {
        // channel must have been closed
        val cause = ClosedChannelException()
        interests.forEach { it.cont.resumeWithException(cause) }
        interests.clear()
        continue
      }

      val readyOps = key.readyOps()
      logger.debug("Selector {}: Channel {} selected for interests {}", System.identityHashCode(selector),
        System.identityHashCode(key.channel()), readyOps)
      var remainingOps = 0

      val it = interests.iterator()
      while (it.hasNext()) {
        val interest = it.next()
        // if any of the interests are set, then resume the continuation
        if ((interest.ops and readyOps) != 0) {
          interest.cont.resume(Unit)
          it.remove()
          awoken++
        } else {
          remainingOps = remainingOps or interest.ops
        }
      }
      key.interestOps(remainingOps)
      if (interests.isEmpty()) {
        registeredKeys.remove(key)
        key.cancel()
      }
    }
    return awoken
  }

  private fun cancelMissingRegistrations(): Int {
    val selectorKeys = selector.keys()
    if (selectorKeys.size == registeredKeys.size) {
      // assume sets of the same size contain the same members
      return 0
    }

    // There should only be less keys in the selector, as keys will vanish after cancellation via closing their channel
    check(selectorKeys.size < registeredKeys.size) { "More registered keys than are outstanding" }

    var processed = 0
    registeredKeys.removeIf { key ->
      if (selectorKeys.contains(key)) {
        false
      } else {
        val cause = ClosedChannelException()
        @Suppress("UNCHECKED_CAST")
        val interests = key.attachment() as ArrayList<SelectionInterest>
        for (interest in interests) {
          interest.cont.resumeWithException(cause)
          processed++
        }
        interests.clear()
        key.cancel()
        true
      }
    }
    return processed
  }

  private fun cancelAll(e: Throwable) {
    while (true) {
      val it = pendingInterests.poll() ?: break
      it.cont.resumeWithException(e)
    }
    registeredKeys.forEach { key ->
      @Suppress("UNCHECKED_CAST")
      (key.attachment() as ArrayList<SelectionInterest>).forEach { it.cont.resumeWithException(e) }
    }
    registeredKeys.clear()
    while (true) {
      val it = pendingCancellations.poll() ?: break
      it.cont.resumeWithException(e)
    }
    outstandingTasks.set(0)
  }

  private data class SelectionInterest(
    val cont: CancellableContinuation<Unit>,
    val channel: SelectableChannel,
    val ops: Int
  )

  private data class SelectionCancellation(
    val channel: SelectableChannel,
    val cause: Throwable?,
    val cont: CancellableContinuation<Boolean>
  )
}
