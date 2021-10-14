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

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.RuntimeException

private val NOOP_EXCEPTION_HANDLER = CoroutineExceptionHandler { _, _ -> }

@ExperimentalCoroutinesApi
internal class RetryableTest {

  @Test
  fun shouldNotRetryIfFirstAttemptReturns() = runBlocking {
    var attempts = 0
    val result = retry(500) {
      attempts++
      "done"
    }
    assertEquals("done", result)
    assertEquals(1, attempts)
  }

  @Test
  fun shouldRetryUntilSuccess() = runBlocking {
    var attempts = 0
    val result = retry(100) { i ->
      attempts++
      delay(470)
      "done $i"
    }
    assertEquals("done 1", result)
    assertEquals(5, attempts)
  }

  @Test
  fun shouldReturnAnySuccess() = runBlocking {
    var attempts = 0
    val result = retry(25) { i ->
      attempts++
      delay(if (i == 4) 60 else 1000)
      "done $i"
    }
    assertEquals("done 4", result)
    assertTrue(attempts > 4)
  }

  @Test
  fun shouldStopRetryingAfterMaxAttempts() = runBlocking {
    var attempts = 0
    val result = retry(50, 3) { i ->
      attempts++
      delay(250)
      "done $i"
    }
    assertEquals("done 1", result)
    assertEquals(3, attempts)
  }

  @Test
  fun shouldReturnNullIfAllAttemptsFail() = runBlocking {
    var attempts = 0
    val result = retry(50, 3) {
      attempts++
      delay(250)
      null
    }
    assertNull(result)
    assertEquals(3, attempts)
  }

  @Test
  fun shouldThrowIfAttemptThrows() {
    var attempts = 0
    val e = assertThrows<RuntimeException> {
      runBlocking(NOOP_EXCEPTION_HANDLER) {
        retry(25) { i ->
          attempts++
          if (i == 4) {
            throw RuntimeException("catch me")
          }
          delay(1000)
        }
      }
    }
    assertEquals("catch me", e.message)
    assertEquals(4, attempts)
  }
}
