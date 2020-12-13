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
