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
package org.apache.tuweni.concurrent.coroutines

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Retry a suspending block until a non-null result is obtained.
 *
 * @param retryDelay the delay between each attempt
 * @param block the suspending block to be executed
 * @return the first non-null result
 */
@ExperimentalCoroutinesApi
suspend fun <R> CoroutineScope.retry(
  retryDelay: Long,
  block: suspend (Int) -> R?
): R = retry({ retryDelay }, block)!!

/**
 * Retry a suspending block until a non-null result is obtained.
 *
 * @param retryDelay the delay between each attempt
 * @param maxRetries the maximum number of attempts
 * @param block the suspending block to be executed
 * @return the first non-null result, or `null` if all attempts fail
 */
@ExperimentalCoroutinesApi
suspend fun <R> CoroutineScope.retry(
  retryDelay: Long,
  maxRetries: Int,
  block: suspend (Int) -> R?
): R? = retry({ i -> if (i > maxRetries) null else retryDelay }, block)

/**
 * Retry a suspending block until a non-null result is obtained.
 *
 * @param retryDelay a function returning the delay that should follow each attempt, or `null` if no further attempts
 *         should be made
 * @param block the suspending block to be executed
 * @return the first non-null result, or `null` if all attempts fail
 */
@ExperimentalCoroutinesApi
suspend fun <R> CoroutineScope.retry(
  retryDelay: (Int) -> Long?,
  block: suspend (Int) -> R?
): R? {
  val jobs = mutableSetOf<Job>()
  val result = CompletableDeferred<R?>()
  result.invokeOnCompletion { jobs.forEach { job -> job.cancel() } }

  var stopped = false
  var i = 1
  while (true) {
    val attempt = i
    val delayTime = retryDelay(attempt) ?: break
    val deferred = async { block(attempt) }
    deferred.invokeOnCompletion { e ->
      try {
        jobs.remove(deferred)
        if (e is CancellationException) {
          // ignore
          return@invokeOnCompletion
        }
        if (e != null) {
          result.completeExceptionally(e)
        } else {
          deferred.getCompleted()?.let { r -> result.complete(r) }
          if (stopped && jobs.isEmpty()) {
            result.complete(null)
          }
        }
      } catch (e: Throwable) {
        result.completeExceptionally(e)
      }
    }
    jobs.add(deferred)

    val r = withTimeoutOrNull(delayTime) { result.await() }
    if (r != null) {
      return r
    }
    ++i
  }
  stopped = true
  if (jobs.isEmpty()) {
    return null
  }
  return result.await()
}

/**
 * Cancel and retry a suspending block until a non-null result is obtained.
 *
 * @param timeout the delay before re-attempting
 * @param block the suspending block to be executed
 * @return the first non-null result
 */
suspend fun <R> timeoutAndRetry(
  timeout: Long,
  block: suspend (Int) -> R?
): R = timeoutAndRetry({ timeout }, block)!!

/**
 * Cancel and retry a suspending block until a non-null result is obtained.
 *
 * @param timeout the delay before re-attempting
 * @param maxRetries the maximum number of attempts
 * @param block the suspending block to be executed
 * @return the first non-null result, or `null` if all attempts fail
 */
suspend fun <R> timeoutAndRetry(
  timeout: Long,
  maxRetries: Int,
  block: suspend (Int) -> R?
): R? = timeoutAndRetry({ i -> if (i >= maxRetries) null else timeout }, block)

/**
 * Cancel and retry a suspending block until a non-null result is obtained.
 *
 * @param timeout a function returning the delay that should follow each attempt, or `null` if no further attempts
 *         should be made
 * @param block the suspending block to be executed
 * @return the first non-null result, or `null` if all attempts fail
 */
suspend fun <R> timeoutAndRetry(
  timeout: (Int) -> Long?,
  block: suspend (Int) -> R?
): R? {
  var i = 1
  while (true) {
    val attempt = i
    val time = timeout(attempt) ?: return null
    val r = withTimeoutOrNull(time) { block(attempt) }
    if (r != null) {
      return r
    }
    ++i
  }
}
