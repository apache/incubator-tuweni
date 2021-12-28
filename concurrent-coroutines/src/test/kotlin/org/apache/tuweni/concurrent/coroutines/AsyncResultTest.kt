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
