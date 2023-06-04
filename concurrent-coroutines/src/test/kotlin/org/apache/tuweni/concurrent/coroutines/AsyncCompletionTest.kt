// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.concurrent.coroutines

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.AsyncCompletion
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class AsyncCompletionTest {

  @Test
  fun testCompleted() = runBlocking {
    val completion = asyncCompletion {
    }
    completion.await()
    assertTrue(completion.isDone)
    assertFalse(completion.isCompletedExceptionally)
  }

  @Test
  fun testFailed() = runBlocking {
    val completion = asyncCompletion {
      throw IllegalArgumentException("foo")
    }
    assertThrows<IllegalArgumentException> {
      runBlocking {
        completion.await()
      }
    }
    assertTrue(completion.isDone)
    assertTrue(completion.isCompletedExceptionally)
  }

  @Test
  fun testCanceled() = runBlocking {
    val completion = asyncCompletion {
      delay(5000)
    }
    completion.cancel()
    assertTrue(completion.isDone)
    assertTrue(completion.isCancelled)
  }

  @Test
  fun testDeferredAlreadyCompleted() = runBlocking {
    val completion = asyncCompletion {
    }
    completion.await()
    val deferred = completion.asDeferred()
    assertTrue(deferred.isCompleted)
    assertFalse(completion.isCompletedExceptionally)
  }

  @Test
  fun testDeferredCompleted() = runBlocking {
    val completion = asyncCompletion {
    }
    val deferred = completion.asDeferred()
    deferred.await()
    assertTrue(completion.isDone)
    assertFalse(completion.isCompletedExceptionally)
  }

  @Test
  fun testDeferredFailed() = runBlocking {
    val completion = asyncCompletion {
      throw IllegalArgumentException("foo")
    }
    val deferred = completion.asDeferred()
    assertThrows<IllegalArgumentException> {
      runBlocking {
        deferred.await()
      }
    }
    assertTrue(completion.isDone)
    assertTrue(completion.isCompletedExceptionally)
  }

  @Test
  fun testDeferredCanceled() = runBlocking {
    val completion = asyncCompletion {
      delay(5000)
    }
    val deferred = completion.asDeferred()
    deferred.cancel()
    assertTrue(completion.isDone)
    assertTrue(completion.isCancelled)
  }

  @Test
  fun deferredToAsyncCompletion() = runBlocking {
    val completion = async {
    }.asAsyncCompletion()
    completion.await()
    assertTrue(completion.isDone)
    assertFalse(completion.isCompletedExceptionally)
  }

  @Test
  fun deferredFailedToAsyncCompletion() = runBlocking {
    var completion: AsyncCompletion? = null
    assertThrows<IllegalArgumentException> {
      runBlocking {
        completion = async {
          delay(1000)
          throw IllegalArgumentException("foo")
        }.asAsyncCompletion()

        completion!!.await()
      }
    }
    assertTrue(completion!!.isDone)
    assertTrue(completion!!.isCompletedExceptionally)
  }
}
