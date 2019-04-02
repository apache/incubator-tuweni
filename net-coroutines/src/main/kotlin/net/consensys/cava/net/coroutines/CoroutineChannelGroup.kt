/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.net.coroutines

import org.logl.LoggerProvider
import java.nio.channels.Channel
import java.nio.channels.SelectableChannel
import java.nio.channels.ShutdownChannelGroupException
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A common co-routine channel group.
 *
 * This group is created with a selector per available processors.
 */
val CommonCoroutineGroup: CoroutineChannelGroup = CoroutineChannelGroup.open()

/**
 * A grouping of co-routine channels for the purpose of resource sharing.
 *
 * A co-routine channel group encapsulates the mechanics required to handle completion of suspended I/O operations
 * initiated on channels bound to the group.
 *
 * @author Chris Leishman - https://cleishm.github.io/
 */
sealed class CoroutineChannelGroup {

  companion object {
    /**
     * Create a co-routine channel group.
     *
     * @param nSelectors The number of selectors that should be active in the group. Defaults to one per available
     *         system processor.
     * @param executor The thread pool for running selectors. Defaults to a fixed size thread pool, with one thread
     *         per selector.
     * @param loggerProvider A provider for logger instances.
     * @param selectTimeout The maximum time the selection operation will wait before checking for closed channels.
     * @param idleTimeout The minimum idle time before the selection loop of a selector exits.
     * @return A co-routine channel group.
     */
    fun open(
      nSelectors: Int = Runtime.getRuntime().availableProcessors(),
      executor: Executor = Executors.newFixedThreadPool(nSelectors, CoroutineSelector.DEFAULT_THREAD_FACTORY),
      loggerProvider: LoggerProvider = LoggerProvider.nullProvider(),
      selectTimeout: Long = 1000,
      idleTimeout: Long = 10000
    ): CoroutineChannelGroup {
      require(nSelectors > 0) { "nSelectors must be larger than zero" }
      return CoroutineSelectorChannelGroup(nSelectors, executor, loggerProvider, selectTimeout, idleTimeout)
    }
  }

  internal abstract fun register(channel: Channel): Boolean

  internal abstract fun deRegister(channel: Channel): Boolean

  internal abstract fun selectorFor(channel: SelectableChannel): CoroutineSelector

  internal suspend fun select(channel: SelectableChannel, ops: Int) {
    selectorFor(channel).select(channel, ops)
  }

  /**
   * Check if the group has been shutdown.
   *
   * @return `true` if the group is shutdown.
   */
  abstract val isShutdown: Boolean

  /**
   * Check if the group has terminated.
   *
   * @return `true` if the group has terminated.
   */
  abstract val isTerminated: Boolean

  /**
   * Shuts down the group.
   */
  abstract fun shutdown()

  /**
   * Shuts down the group and closes all open channels in the group.
   */
  abstract fun shutdownNow()

  /**
   * Suspend until the group has terminated.
   */
  abstract suspend fun awaitTermination()
}

internal class CoroutineSelectorChannelGroup(
  nSelectors: Int,
  private val executor: Executor,
  loggerProvider: LoggerProvider,
  selectTimeout: Long,
  idleTimeout: Long
) : CoroutineChannelGroup() {

  private val logger = loggerProvider.getLogger(CoroutineChannelGroup::class.java)

  private var selectors: Array<CoroutineSelector>? = Array(nSelectors) {
    CoroutineSelector.open(executor, loggerProvider, selectTimeout, idleTimeout)
  }
  private val channels = Collections.synchronizedSet(Collections.newSetFromMap(WeakHashMap<Channel, Boolean>()))
  @Volatile
  override var isShutdown: Boolean = false
  @Volatile
  private var wasTerminated: Boolean = false
  private val pendingTermination = ConcurrentLinkedQueue<Continuation<Unit>>()

  override fun register(channel: Channel): Boolean {
    if (isShutdown) {
      throw ShutdownChannelGroupException()
    }
    val added = channels.add(channel)
    if (added) {
      logger.debug("Added channel {} to group {}", System.identityHashCode(channel),
        System.identityHashCode(this))
    }
    return added
  }

  override fun deRegister(channel: Channel): Boolean {
    val removed = channels.remove(channel)
    if (removed) {
      logger.debug("Removed channel {} from group {}", System.identityHashCode(channel),
        System.identityHashCode(this))
      if (isShutdown && channels.isEmpty()) {
        terminate()
      }
    }
    return removed
  }

  override fun selectorFor(channel: SelectableChannel): CoroutineSelector {
    val selectors = this.selectors ?: throw IllegalStateException(
      "Access to terminated ChannelGroup by unregistered channel")
    return selectors[System.identityHashCode(channel).rem(selectors.size)]
  }

  override val isTerminated: Boolean
    get() {
      if (isShutdown && !wasTerminated && channels.isEmpty()) {
        terminate()
        return true
      }
      return wasTerminated
    }

  override fun shutdown() {
    isShutdown = true
    logger.debug("Shutdown channel group {}", System.identityHashCode(this))
    if (!wasTerminated && channels.isEmpty()) {
      terminate()
    }
  }

  override fun shutdownNow() {
    shutdown()
    channels.forEach { it.close() }
  }

  override suspend fun awaitTermination() {
    if (wasTerminated) {
      return
    }
    suspendCoroutine<Unit> { cont ->
      pendingTermination.add(cont)
      if (isShutdown && channels.isEmpty()) {
        terminate()
      }
    }
  }

  private fun terminate() {
    check(isShutdown)
    wasTerminated = true
    logger.debug("Terminated channel group {}", System.identityHashCode(this))
    while (true) {
      (pendingTermination.poll() ?: break).resume(Unit)
    }
    val selectors = this.selectors
    this.selectors = null
    selectors?.let { it.map { selector -> selector.close() } }
  }
}
