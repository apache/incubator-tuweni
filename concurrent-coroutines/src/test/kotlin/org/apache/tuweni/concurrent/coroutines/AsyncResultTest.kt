// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.concurrent.coroutines

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.tuweni.concurrent.AsyncResult
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@ExperimentalCoroutinesApi
class AsyncResultTest {

  @Test
  fun testCompleted() = runBlocking {
    val result = asyncResult {
    }
    result.await()
    assertTrue(result.isDone)
    assertFalse(result.isCompletedExceptionally)
  }

  @Test
  fun testFailed() = runBlocking {
    val result = asyncResult<String> {
      throw IllegalArgumentException("foo")
    }
    assertThrows<IllegalArgumentException> {
      runBlocking {
        result.await()
      }
    }
    assertTrue(result.isDone)
    assertTrue(result.isCompletedExceptionally)
  }

  @Test
  fun testCanceled() = runBlocking {
    val result = asyncResult {
      delay(5000)
    }
    result.cancel()
    assertTrue(result.isDone)
    assertTrue(result.isCancelled)
  }

  @Test
  fun testDeferredAlreadyCompleted() = runBlocking {
    val result = asyncResult {
    }
    result.await()
    val deferred = result.asDeferred()
    assertTrue(deferred.isCompleted)
    assertFalse(result.isCompletedExceptionally)
  }

  @Test
  fun testDeferredCompleted() = runBlocking {
    val result = asyncResult {
    }
    val deferred = result.asDeferred()
    deferred.await()
    assertTrue(result.isDone)
    assertFalse(result.isCompletedExceptionally)
  }

  @Test
  fun testDeferredFailed() = runBlocking {
    val result = asyncResult<String> {
      throw IllegalArgumentException("foo")
    }
    val deferred = result.asDeferred()
    assertThrows<IllegalArgumentException> {
      runBlocking {
        deferred.await()
      }
    }
    assertTrue(result.isDone)
    assertTrue(result.isCompletedExceptionally)
  }

  @Test
  fun testDeferredCanceled() = runBlocking {
    val result = asyncResult {
      delay(5000)
    }
    val deferred = result.asDeferred()
    deferred.cancel()
    assertTrue(result.isDone)
    assertTrue(result.isCancelled)
  }

  @Test
  fun deferredToAsyncResult() = runBlocking {
    val result = async {
    }.asAsyncResult()
    result.await()
    assertTrue(result.isDone)
    assertFalse(result.isCompletedExceptionally)
  }

  @Test
  fun deferredFailedToAsyncResult() = runBlocking {
    var result: AsyncResult<String>? = null
    assertThrows<IllegalArgumentException> {
      runBlocking {
        result = async {
          delay(1000)
          throw IllegalArgumentException("foo")
        }.asAsyncResult()

        result!!.await()
      }
    }
    assertTrue(result!!.isDone)
    assertTrue(result!!.isCompletedExceptionally)
  }
}
