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
package net.consensys.cava.concurrent.coroutines

import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * A co-routine synchronization aid that allows co-routines to wait until a set of operations being performed
 * has completed.
 *
 * The latch is initialized with a given count. If the latch count is greater than zero, the `await()` method will
 * suspend until the count reaches zero due to invocations of the `countDown()` method, at which point all suspended
 * co-routines will be resumed.
 *
 * Unlike the Java `CountDownLatch`, this latch allows the count to be increased via invocation of the `countUp()`
 * method. Increasing the count from zero will result in calls to `await()` suspending again. Note that the count may
 * be negative, requiring multiple calls to `countUp()` before calls to `await()` suspend.
 *
 * @param initial The initial count of the latch, which may be positive, zero, or negative.
 * @constructor A latch.
 */
class CoroutineLatch(initial: Int) {

  private val atomicCount = AtomicInteger(initial)
  private var waitingCoroutines = mutableListOf<Continuation<Unit>>()

  /**
   * The current latch count.
   */
  val count: Int
    get() = atomicCount.get()

  /**
   * Indicates if the latch is open (`count <= 0`).
   */
  val isOpen: Boolean
    get() = atomicCount.get() <= 0

  /**
   * Decrease the latch count, potentially opening the latch and awakening suspending co-routines.
   *
   * @return `true` if the latch was opened as a result of this invocation.
   */
  fun countDown(): Boolean {
    var toAwaken: List<Continuation<Unit>>? = null
    synchronized(this) {
      if (atomicCount.decrementAndGet() == 0) {
        toAwaken = waitingCoroutines
        waitingCoroutines = mutableListOf()
      }
    }
    toAwaken?.forEach { it.resume(Unit) }
    return toAwaken != null
  }

  /**
   * Increase the latch count, potentially closing the latch.
   *
   * @return `true` if the latch was closed as a result of this invocation.
   */
  fun countUp(): Boolean = atomicCount.incrementAndGet() == 1

  /**
   * Await the latch opening. If already open, return without suspending.
   */
  suspend fun await() {
    if (atomicCount.get() <= 0) {
      return
    }
    suspendCancellableCoroutine { cont: Continuation<Unit> ->
      try {
        var suspended: Boolean
        synchronized(this) {
          suspended = atomicCount.get() > 0
          if (suspended) {
            waitingCoroutines.add(cont)
          }
        }
        if (!suspended) {
          cont.resume(Unit)
        }
      } catch (e: Throwable) {
        cont.resumeWithException(e)
      }
    }
  }
}
