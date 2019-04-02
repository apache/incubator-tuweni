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
package net.consensys.cava.concurrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class DefaultCompletableAsyncResultTest {

  @Test
  void shouldReturnValueFromCompletedResult() throws Exception {
    AsyncResult<String> asyncResult = AsyncResult.completed("Completed");
    assertThat(asyncResult.isDone()).isTrue();
    assertThat(asyncResult.get()).isEqualTo("Completed");
  }

  @Test
  void shouldReturnNullValueFromCompletedResult() throws Exception {
    AsyncResult<String> asyncResult = AsyncResult.completed(null);
    assertThat(asyncResult.isDone()).isTrue();
    assertNull(asyncResult.get());
  }

  @Test
  void shouldReturnExceptionFromExceptionallyCompletedResult() throws Exception {
    Exception exception = new RuntimeException();
    AsyncResult<String> asyncResult = AsyncResult.exceptional(exception);
    assertThat(asyncResult.isDone()).isTrue();
    assertCompletedWithException(asyncResult, exception);
  }

  @Test
  void isNotDoneUntilCompleted() throws Exception {
    CompletableAsyncResult<String> asyncResult = AsyncResult.incomplete();
    assertThat(asyncResult.isDone()).isFalse();
    asyncResult.complete("Completed");
    assertThat(asyncResult.isDone()).isTrue();
    assertThat(asyncResult.get()).isEqualTo("Completed");
  }

  @Test
  void suppliesAsyncResultWhenCompleted() throws Exception {
    CompletableAsyncResult<String> asyncResult = AsyncResult.incomplete();
    AsyncResult<String> asyncResult2 = asyncResult.then(value -> {
      assertThat(value).isEqualTo("Completed1");
      return AsyncResult.completed("Completed2");
    });
    assertThat(asyncResult2.isDone()).isFalse();
    asyncResult.complete("Completed1");
    assertThat(asyncResult2.isDone()).isTrue();
    assertThat(asyncResult2.get()).isEqualTo("Completed2");
  }

  @Test
  void completesExceptionallyWhenSuppliedResultCompletesExceptionally() throws Exception {
    Exception exception = new RuntimeException();
    CompletableAsyncResult<String> asyncResult = AsyncResult.incomplete();
    AsyncResult<String> asyncResult2 = asyncResult.then(value -> AsyncResult.exceptional(exception));
    assertThat(asyncResult2.isDone()).isFalse();
    asyncResult.complete("Complete");
    assertThat(asyncResult2.isDone()).isTrue();
    assertCompletedWithException(asyncResult2, exception);
  }

  @Test
  void completesExceptionallyWhenSupplierThrows() throws Exception {
    RuntimeException exception = new RuntimeException();
    CompletableAsyncResult<String> asyncResult = AsyncResult.incomplete();
    AsyncResult<String> asyncResult2 = asyncResult.then(value -> {
      throw exception;
    });
    assertThat(asyncResult2.isDone()).isFalse();
    asyncResult.complete("Complete");
    assertThat(asyncResult2.isDone()).isTrue();
    assertCompletedWithException(asyncResult2, exception);
  }

  @Test
  void doesntInvokeSupplierIfCompletingExceptionally() throws Exception {
    RuntimeException exception = new RuntimeException();
    CompletableAsyncResult<String> asyncResult = AsyncResult.incomplete();
    AsyncResult<String> asyncResult2 = asyncResult.then(value -> {
      fail("should not be invoked");
      throw new RuntimeException();
    });
    assertThat(asyncResult2.isDone()).isFalse();
    asyncResult.completeExceptionally(exception);
    assertThat(asyncResult2.isDone()).isTrue();
    assertCompletedWithException(asyncResult2, exception);
  }

  @Test
  void completesWhenAllInCollectionComplete() {
    CompletableAsyncResult<String> asyncResult1 = AsyncResult.incomplete();
    CompletableAsyncResult<String> asyncResult2 = AsyncResult.incomplete();
    Collection<AsyncResult<String>> list = Arrays.asList(asyncResult1, asyncResult2);

    AsyncCompletion completion = AsyncResult.allOf(list);
    assertThat(completion.isDone()).isFalse();

    asyncResult1.complete("one");
    assertThat(completion.isDone()).isFalse();
    asyncResult2.complete("two");
    assertThat(completion.isDone()).isTrue();
  }

  @Test
  void completesWithExceptionWhenAnyInCollectionFail() throws Exception {
    CompletableAsyncResult<String> asyncResult1 = AsyncResult.incomplete();
    CompletableAsyncResult<Integer> asyncResult2 = AsyncResult.incomplete();

    AsyncCompletion completion = AsyncResult.allOf(asyncResult1, asyncResult2);
    assertThat(completion.isDone()).isFalse();

    Exception exception = new RuntimeException();
    asyncResult1.completeExceptionally(exception);
    assertThat(completion.isDone()).isFalse();

    asyncResult2.complete(2);
    assertThat(completion.isDone()).isTrue();
    assertCompletedWithException(completion, exception);
  }

  @Test
  void completesWhenCombinedComplete() throws Exception {
    CompletableAsyncResult<String> asyncResult1 = AsyncResult.incomplete();
    CompletableAsyncResult<String> asyncResult2 = AsyncResult.incomplete();
    Collection<AsyncResult<String>> list = Arrays.asList(asyncResult1, asyncResult2);

    AsyncResult<List<String>> result = AsyncResult.combine(list);
    assertThat(result.isDone()).isFalse();

    asyncResult1.complete("one");
    assertThat(result.isDone()).isFalse();
    asyncResult2.complete("two");
    assertThat(result.isDone()).isTrue();

    List<String> strings = result.get();
    assertThat(strings).isEqualTo(Arrays.asList("one", "two"));
  }

  @Test
  void invokesComposedWhenCanceled() {
    CompletableAsyncResult<String> asyncResult = AsyncResult.incomplete();

    AtomicReference<Throwable> completedThrowable = new AtomicReference<>();
    AsyncResult<String> downstreamAsyncResult =
        asyncResult.whenComplete((result, throwable) -> completedThrowable.set(throwable));

    asyncResult.cancel();
    assertThat(asyncResult.isDone()).isTrue();
    assertThat(asyncResult.isCancelled()).isTrue();
    assertThat(asyncResult.isCompletedExceptionally()).isTrue();

    assertThat(downstreamAsyncResult.isDone()).isTrue();
    assertThat(downstreamAsyncResult.isCancelled()).isFalse();
    assertThat(downstreamAsyncResult.isCompletedExceptionally()).isTrue();

    assertThat(completedThrowable.get()).isInstanceOf(CancellationException.class);
  }

  private void assertCompletedWithException(AsyncResult<?> asyncResult, Exception exception) throws Exception {
    try {
      asyncResult.get();
      fail("Expected exception not thrown");
    } catch (CompletionException ex) {
      assertThat(ex.getCause()).isSameAs(exception);
    }
  }

  private void assertCompletedWithException(AsyncCompletion completion, Exception exception) throws Exception {
    try {
      completion.join();
      fail("Expected exception not thrown");
    } catch (CompletionException ex) {
      assertThat(ex.getCause()).isSameAs(exception);
    }
  }
}
