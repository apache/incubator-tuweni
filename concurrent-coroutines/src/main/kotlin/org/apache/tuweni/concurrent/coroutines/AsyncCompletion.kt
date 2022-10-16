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

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.apache.tuweni.concurrent.AsyncCompletion
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletionException
import java.util.function.Consumer
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Starts new co-routine and returns its result as an implementation of [AsyncCompletion].
 * The running co-routine is cancelled when the resulting future is cancelled or otherwise completed.
 *
 * Co-routine context is inherited from a [CoroutineScope], additional context elements can be specified with [context]
 * argument. If the context does not have any dispatcher nor any other [ContinuationInterceptor], then
 * [Dispatchers.Default] is used. The parent job is inherited from a [CoroutineScope] as well, but it can also be
 * overridden with corresponding [coroutineContext] element.
 *
 * By default, the co-routine is immediately scheduled for execution. Other options can be specified via `start`
 * parameter. See [CoroutineStart] for details. A value of [CoroutineStart.LAZY] is not supported (since
 * `AsyncResult` framework does not provide the corresponding capability) and produces [IllegalArgumentException].
 *
 * See [newCoroutineContext][CoroutineScope.newCoroutineContext] for a description of debugging facilities that are
 * available for newly created co-routine.
 *
 * @param context Additional to [CoroutineScope.coroutineContext] context of the coroutine.
 * @param start Co-routine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block The co-routine code.
 */
fun CoroutineScope.asyncCompletion(
  context: CoroutineContext = Dispatchers.Default,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  block: suspend CoroutineScope.() -> Unit
): AsyncCompletion {
  val completion = AsyncCompletion.incomplete()
  try {
    launch(context, start) {
      try {
        block()
        completion.complete()
      } catch (t: Throwable) {
        completion.completeExceptionally(t)
      }
    }
  } catch (t: Throwable) {
    completion.completeExceptionally(t)
  }
  return completion
}

/**
 * Converts this deferred value to a [AsyncCompletion].
 * The deferred value is cancelled when the returned [AsyncCompletion] is cancelled or otherwise completed.
 */
fun Deferred<Unit>.asAsyncCompletion(): AsyncCompletion {
  val asyncCompletion = AsyncCompletion.incomplete()
  asyncCompletion.whenComplete { cancel() }
  invokeOnCompletion {
    if (it != null) {
      asyncCompletion.completeExceptionally(it)
    } else {
      asyncCompletion.complete()
    }
  }
  return asyncCompletion
}

/**
 * Converts this job to a [AsyncCompletion].
 * The job is cancelled when the returned [AsyncCompletion] is cancelled or otherwise completed.
 */
fun Job.asAsyncCompletion(): AsyncCompletion {
  val asyncCompletion = AsyncCompletion.incomplete()
  asyncCompletion.whenComplete { cancel() }
  invokeOnCompletion {
    try {
      asyncCompletion.complete()
    } catch (exception: Exception) {
      asyncCompletion.completeExceptionally(exception)
    }
  }
  return asyncCompletion
}

/**
 * Converts this [AsyncCompletion] to an instance of [Deferred].
 * The [AsyncCompletion] is cancelled when the resulting deferred is cancelled.
 */
fun AsyncCompletion.asDeferred(): Deferred<Unit> {
  // Fast path if already completed
  if (isDone) {
    return try {
      CompletableDeferred(join())
    } catch (e: Throwable) {
      // unwrap original cause from CompletionException
      val original = (e as? CompletionException)?.cause ?: e
      CompletableDeferred<Unit>().also { it.completeExceptionally(original) }
    }
  }
  val result = CompletableDeferred<Unit>()
  whenComplete { exception ->
    if (exception == null) {
      result.complete(Unit)
    } else {
      result.completeExceptionally(exception)
    }
  }
  result.invokeOnCompletion { this.cancel() }
  return result
}

/**
 * Awaits for completion of the [AsyncCompletion] without blocking a thread.
 *
 * This suspending function is cancellable. If the [Job] of the current coroutine is cancelled or completed while this
 * suspending function is waiting, this function stops waiting for the [AsyncCompletion] and immediately resumes with
 * [CancellationException].
 *
 * Note, that [AsyncCompletion] does not support prompt removal of listeners, so on cancellation of this wait a few
 * small objects will remain in the [AsyncCompletion] stack of completion actions until it completes itself. However,
 * care is taken to clear the reference to the waiting coroutine itself, so that its memory can be released even if the
 * [AsyncCompletion] never completes.
 */
suspend fun AsyncCompletion.await() {
  // fast path when CompletableFuture is already done (does not suspend)
  if (isDone) {
    try {
      return join()
    } catch (e: CompletionException) {
      throw e.cause ?: e // unwrap original cause from CompletionException
    }
  }
  // slow path -- suspend
  return suspendCancellableCoroutine { cont: CancellableContinuation<Unit> ->
    val consumer = ContinuationConsumer(cont)
    whenComplete(consumer)
    cont.invokeOnCancellation {
      consumer.cont = null // shall clear reference to continuation
    }
  }
}

private class ContinuationConsumer(
  @Volatile @JvmField
  var cont: Continuation<Unit>?
) : Consumer<Throwable?> {
  override fun accept(exception: Throwable?) {
    val cont = this.cont ?: return // atomically read current value unless null
    if (exception == null) {
      // the future has been completed normally
      cont.resume(Unit)
    } else {
      // the future has completed with an exception, unwrap it to provide consistent view of .await() result and to propagate only original exception
      cont.resumeWithException((exception as? CompletionException)?.cause ?: exception)
    }
  }
}
