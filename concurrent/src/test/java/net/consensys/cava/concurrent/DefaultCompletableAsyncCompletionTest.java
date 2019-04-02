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
package org.apache.tuweni.concurrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class DefaultCompletableAsyncCompletionTest {

  @Test
  void shouldReturnValueFromCompletedResult() {
    AsyncCompletion completion = AsyncCompletion.completed();
    assertThat(completion.isDone()).isTrue();
  }

  @Test
  void shouldReturnExceptionFromExceptionallyCompletedResult() throws Exception {
    Exception exception = new RuntimeException();
    AsyncCompletion completion = AsyncCompletion.exceptional(exception);
    assertThat(completion.isDone()).isTrue();
    assertCompletedWithException(completion, exception);
  }

  @Test
  void isNotDoneUntilCompleted() {
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    assertThat(completion.isDone()).isFalse();
    completion.complete();
    assertThat(completion.isDone()).isTrue();
  }

  @Test
  void invokesContinuationFunctionWhenCompleted() throws Exception {
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    AsyncResult<String> asyncResult = completion.then(() -> AsyncResult.completed("Completed"));
    assertThat(asyncResult.isDone()).isFalse();
    completion.complete();
    assertThat(asyncResult.isDone()).isTrue();
    assertThat(asyncResult.get()).isEqualTo("Completed");
  }

  @Test
  void completesExceptionallyWhenContinuationResultCompletesExceptionally() throws Exception {
    Exception exception = new RuntimeException();
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    AsyncResult<String> asyncResult = completion.then(() -> AsyncResult.exceptional(exception));
    assertThat(asyncResult.isDone()).isFalse();
    completion.complete();
    assertThat(asyncResult.isDone()).isTrue();
    assertCompletedWithException(asyncResult, exception);
  }

  @Test
  void completesExceptionallyWhenContinuationFunctionThrows() throws Exception {
    RuntimeException exception = new RuntimeException();
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    AsyncResult<String> asyncResult = completion.then(() -> {
      throw exception;
    });
    assertThat(asyncResult.isDone()).isFalse();
    completion.complete();
    assertThat(asyncResult.isDone()).isTrue();
    assertCompletedWithException(asyncResult, exception);
  }

  @Test
  void doesntInvokeContinuationFunctionIfCompletingExceptionally() throws Exception {
    RuntimeException exception = new RuntimeException();
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    AsyncResult<String> asyncResult = completion.then(() -> {
      fail("should not be invoked");
      throw new RuntimeException();
    });
    assertThat(asyncResult.isDone()).isFalse();
    completion.completeExceptionally(exception);
    assertThat(asyncResult.isDone()).isTrue();
    assertCompletedWithException(asyncResult, exception);
  }

  @Test
  void completesWhenComposedCompletionCompletes() {
    CompletableAsyncCompletion completion1 = AsyncCompletion.incomplete();
    CompletableAsyncCompletion completion2 = AsyncCompletion.incomplete();

    AtomicBoolean composed = new AtomicBoolean(false);
    AsyncCompletion result = completion1.thenCompose(() -> {
      composed.set(true);
      return completion2;
    });
    assertThat(result.isDone()).isFalse();
    assertThat(composed.get()).isFalse();

    completion1.complete();
    assertThat(result.isDone()).isFalse();
    assertThat(composed.get()).isTrue();

    completion2.complete();
    assertThat(result.isDone()).isTrue();
  }

  @Test
  void completesExceptionallyWhenComposedCompletionThrows() throws Exception {
    CompletableAsyncCompletion completion1 = AsyncCompletion.incomplete();
    RuntimeException exception = new RuntimeException();

    AsyncCompletion result = completion1.thenCompose(() -> {
      throw exception;
    });
    assertThat(result.isDone()).isFalse();

    completion1.complete();
    assertCompletedWithException(result, exception);
  }

  @Test
  void completesExceptionallyWhenComposedCompletionCompletesExceptionally() throws Exception {
    CompletableAsyncCompletion completion1 = AsyncCompletion.incomplete();
    CompletableAsyncCompletion completion2 = AsyncCompletion.incomplete();

    AtomicBoolean composed = new AtomicBoolean(false);
    AsyncCompletion result = completion1.thenCompose(() -> {
      composed.set(true);
      return completion2;
    });
    assertThat(result.isDone()).isFalse();
    assertThat(composed.get()).isFalse();

    completion1.complete();
    assertThat(result.isDone()).isFalse();
    assertThat(composed.get()).isTrue();

    RuntimeException exception = new RuntimeException();
    completion2.completeExceptionally(exception);
    assertThat(result.isDone()).isTrue();
    assertCompletedWithException(result, exception);
  }

  @Test
  void completesExceptionallyWhenComposerCompletesExceptionally() throws Exception {
    CompletableAsyncCompletion completion1 = AsyncCompletion.incomplete();
    CompletableAsyncCompletion completion2 = AsyncCompletion.incomplete();

    AtomicBoolean composed = new AtomicBoolean(false);
    AsyncCompletion result = completion1.thenCompose(() -> {
      composed.set(true);
      return completion2;
    });
    assertThat(result.isDone()).isFalse();
    assertThat(composed.get()).isFalse();

    RuntimeException exception = new RuntimeException();
    completion1.completeExceptionally(exception);
    assertThat(result.isDone()).isTrue();
    assertThat(composed.get()).isFalse();
    assertCompletedWithException(result, exception);
  }

  @Test
  void completesWhenCombinedCompletionCompletes() {
    CompletableAsyncCompletion completion1 = AsyncCompletion.incomplete();
    CompletableAsyncCompletion completion2 = AsyncCompletion.incomplete();

    AsyncCompletion result = completion1.thenCombine(completion2);
    assertThat(result.isDone()).isFalse();

    completion1.complete();
    assertThat(result.isDone()).isFalse();

    completion2.complete();
    assertThat(result.isDone()).isTrue();
  }

  @Test
  void completesExceptionallyWhenCombinedCompletionCompletesExceptionally() throws Exception {
    CompletableAsyncCompletion completion1 = AsyncCompletion.incomplete();
    CompletableAsyncCompletion completion2 = AsyncCompletion.incomplete();

    AsyncCompletion result = completion1.thenCombine(completion2);
    assertThat(result.isDone()).isFalse();

    completion1.complete();
    assertThat(result.isDone()).isFalse();

    RuntimeException exception = new RuntimeException();
    completion2.completeExceptionally(exception);
    assertThat(result.isDone()).isTrue();
    assertCompletedWithException(result, exception);
  }

  @Test
  void completesExceptionallyWhenCombinerCompletesExceptionally() throws Exception {
    CompletableAsyncCompletion completion1 = AsyncCompletion.incomplete();
    CompletableAsyncCompletion completion2 = AsyncCompletion.incomplete();

    AsyncCompletion result = completion1.thenCombine(completion2);
    assertThat(result.isDone()).isFalse();

    RuntimeException exception = new RuntimeException();
    completion1.completeExceptionally(exception);
    assertThat(result.isDone()).isTrue();
    assertCompletedWithException(result, exception);
  }

  @Test
  void completesWhenAllInCollectionComplete() {
    CompletableAsyncCompletion completion1 = AsyncCompletion.incomplete();
    CompletableAsyncCompletion completion2 = AsyncCompletion.incomplete();
    Collection<AsyncCompletion> list = Arrays.asList(completion1, completion2);

    AsyncCompletion completion = AsyncCompletion.allOf(list);
    assertThat(completion.isDone()).isFalse();

    completion1.complete();
    assertThat(completion.isDone()).isFalse();
    completion2.complete();
    assertThat(completion.isDone()).isTrue();
  }

  @Test
  void completesWithExceptionWhenAnyInCollectionFail() throws Exception {
    CompletableAsyncCompletion completion1 = AsyncCompletion.incomplete();
    CompletableAsyncCompletion completion2 = AsyncCompletion.incomplete();

    AsyncCompletion completion = AsyncCompletion.allOf(completion1, completion2);
    assertThat(completion.isDone()).isFalse();

    Exception exception = new RuntimeException();
    completion1.completeExceptionally(exception);
    assertThat(completion.isDone()).isFalse();

    completion2.complete();
    assertThat(completion.isDone()).isTrue();
    assertCompletedWithException(completion, exception);
  }

  @Test
  void invokesComposedWhenCanceled() {
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();

    AtomicReference<Throwable> completedThrowable = new AtomicReference<>();
    AsyncCompletion downstreamCompletion = completion.whenComplete(completedThrowable::set);

    completion.cancel();
    assertThat(completion.isDone()).isTrue();
    assertThat(completion.isCancelled()).isTrue();
    assertThat(completion.isCompletedExceptionally()).isTrue();

    assertThat(downstreamCompletion.isDone()).isTrue();
    assertThat(downstreamCompletion.isCancelled()).isFalse();
    assertThat(downstreamCompletion.isCompletedExceptionally()).isTrue();

    assertThat(completedThrowable.get()).isInstanceOf(CancellationException.class);
  }

  private void assertCompletedWithException(AsyncCompletion completion, Exception exception) throws Exception {
    try {
      completion.join();
      fail("Expected exception not thrown");
    } catch (CompletionException ex) {
      assertThat(ex.getCause()).isSameAs(exception);
    }
  }

  private void assertCompletedWithException(AsyncResult<?> asyncResult, Exception exception) throws Exception {
    try {
      asyncResult.get();
      fail("Expected exception not thrown");
    } catch (CompletionException ex) {
      assertThat(ex.getCause()).isSameAs(exception);
    }
  }
}
