/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.concurrent;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;

/**
 * A completion that will be complete at a future time.
 */
public interface AsyncCompletion {

  AsyncCompletion COMPLETED = new DefaultCompletableAsyncCompletion(CompletableFuture.completedFuture(null));

  /**
   * Return an already completed completion.
   *
   * @return A completed completion.
   */
  static AsyncCompletion completed() {
    return COMPLETED;
  }

  /**
   * Return an already failed completion, caused by the given exception.
   *
   * @param ex The exception.
   * @return A failed result.
   */
  static AsyncCompletion exceptional(Throwable ex) {
    requireNonNull(ex);
    CompletableAsyncCompletion completion = new DefaultCompletableAsyncCompletion();
    completion.completeExceptionally(ex);
    return completion;
  }

  /**
   * Return an incomplete completion, that can be later completed or failed.
   *
   * @return An incomplete completion.
   */
  static CompletableAsyncCompletion incomplete() {
    return new DefaultCompletableAsyncCompletion();
  }

  /**
   * Returns an {@link AsyncCompletion} that completes when all of the given completions complete. If any completions
   * complete exceptionally, then the resulting completion also completes exceptionally.
   *
   * @param cs The completions to combine.
   * @return A completion.
   */
  static AsyncCompletion allOf(AsyncCompletion... cs) {
    return allOf(Arrays.stream(cs));
  }

  /**
   * Returns an {@link AsyncCompletion} that completes when all of the given completions complete. If any completions
   * complete exceptionally, then the resulting completion also completes exceptionally.
   *
   * @param cs The completions to combine.
   * @return A completion.
   */
  static AsyncCompletion allOf(Collection<AsyncCompletion> cs) {
    return allOf(cs.stream());
  }

  /**
   * Returns an {@link AsyncCompletion} that completes when all of the given completions complete. If any completions
   * complete exceptionally, then the resulting completion also completes exceptionally.
   *
   * @param cs The completions to combine.
   * @return A completion.
   */
  static AsyncCompletion allOf(Stream<AsyncCompletion> cs) {
    @SuppressWarnings("rawtypes")
    java.util.concurrent.CompletableFuture[] completableFutures = cs.map(completion -> {
      java.util.concurrent.CompletableFuture<Void> javaFuture = new java.util.concurrent.CompletableFuture<>();
      completion.whenComplete(ex -> {
        if (ex == null) {
          javaFuture.complete(null);
        } else {
          javaFuture.completeExceptionally(ex);
        }
      });
      return javaFuture;
    }).toArray(java.util.concurrent.CompletableFuture[]::new);
    return new DefaultCompletableAsyncCompletion(java.util.concurrent.CompletableFuture.allOf(completableFutures));
  }

  /**
   * Returns a completion that, after the given function executes on a vertx context and returns a completion, completes
   * when the completion from the function does.
   *
   * @param vertx The vertx context.
   * @param fn The function returning a completion.
   * @return A completion.
   */
  static AsyncCompletion runOnContext(Vertx vertx, Supplier<? extends AsyncCompletion> fn) {
    requireNonNull(fn);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    vertx.runOnContext(ev -> {
      try {
        fn.get().whenComplete(ex2 -> {
          if (ex2 == null) {
            try {
              completion.complete();
            } catch (Throwable ex3) {
              completion.completeExceptionally(ex3);
            }
          } else {
            completion.completeExceptionally(ex2);
          }
        });
      } catch (Throwable ex1) {
        completion.completeExceptionally(ex1);
      }
    });
    return completion;
  }

  /**
   * Returns a completion that completes after the given action executes on a vertx context.
   *
   * <p>
   * Note that the given function is run directly on the context and should not block.
   *
   * @param vertx The vertx context.
   * @param action The action to execute.
   * @return A completion.
   */
  static AsyncCompletion runOnContext(Vertx vertx, Runnable action) {
    requireNonNull(action);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    vertx.runOnContext(ev -> {
      try {
        action.run();
        completion.complete();
      } catch (Throwable ex) {
        completion.completeExceptionally(ex);
      }
    });
    return completion;
  }

  /**
   * Returns a completion that completes after the given blocking action executes asynchronously on
   * {@link ForkJoinPool#commonPool()}.
   *
   * @param action The blocking action to execute.
   * @return A completion.
   */
  static AsyncCompletion executeBlocking(Runnable action) {
    requireNonNull(action);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    ForkJoinPool.commonPool().execute(() -> {
      try {
        action.run();
        completion.complete();
      } catch (Throwable ex) {
        completion.completeExceptionally(ex);
      }
    });
    return completion;
  }

  /**
   * Returns a completion that completes after the given blocking action executes asynchronously on an {@link Executor}.
   *
   * @param executor The executor.
   * @param action The blocking action to execute.
   * @return A completion.
   */
  static AsyncCompletion executeBlocking(Executor executor, Runnable action) {
    requireNonNull(action);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    executor.execute(() -> {
      try {
        action.run();
        completion.complete();
      } catch (Throwable ex) {
        completion.completeExceptionally(ex);
      }
    });
    return completion;
  }

  /**
   * Returns a completion that completes after the given blocking action executes asynchronously on a vertx context.
   *
   * @param vertx The vertx context.
   * @param action The blocking action to execute.
   * @return A completion.
   */
  static AsyncCompletion executeBlocking(Vertx vertx, Runnable action) {
    requireNonNull(action);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    vertx.executeBlocking(future -> {
      action.run();
      future.complete();
    }, false, res -> {
      if (res.succeeded()) {
        completion.complete();
      } else {
        completion.completeExceptionally(res.cause());
      }
    });
    return completion;
  }

  /**
   * Returns a completion that completes after the given blocking action executes asynchronously on a vertx executor.
   *
   * @param executor A vertx executor.
   * @param action The blocking action to execute.
   * @return A completion.
   */
  static AsyncCompletion executeBlocking(WorkerExecutor executor, Runnable action) {
    requireNonNull(action);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    executor.executeBlocking(future -> {
      action.run();
      future.complete();
    }, false, res -> {
      if (res.succeeded()) {
        completion.complete();
      } else {
        completion.completeExceptionally(res.cause());
      }
    });
    return completion;
  }

  /**
   * Returns {@code true} if completed normally, completed exceptionally or cancelled.
   *
   * @return {@code true} if completed.
   */
  boolean isDone();

  /**
   * Returns {@code true} if completed exceptionally or cancelled.
   *
   * @return {@code true} if completed exceptionally or cancelled.
   */
  boolean isCompletedExceptionally();

  /**
   * Attempt to cancel execution of this task.
   *
   * <p>
   * This attempt will fail if the task has already completed, has already been cancelled, or could not be cancelled for
   * some other reason. If successful, and this task has not started when {@code cancel} is called, this task should
   * never run.
   *
   * <p>
   * After this method returns, subsequent calls to {@link #isDone()} will always return {@code true}. Subsequent calls
   * to {@link #isCancelled()} will always return {@code true} if this method returned {@code true}.
   *
   * @return {@code true} if this completion transitioned to a cancelled state.
   */
  boolean cancel();

  /**
   * Returns {@code true} if this task was cancelled before it completed normally.
   *
   * @return {@code true} if completed.
   */
  boolean isCancelled();

  /**
   * Waits if necessary for the computation to complete.
   *
   * @throws CompletionException If the computation threw an exception.
   * @throws InterruptedException If the current thread was interrupted while waiting.
   */
  void join() throws CompletionException, InterruptedException;

  /**
   * Waits if necessary for at most the given time for the computation to complete.
   *
   * @param timeout The maximum time to wait.
   * @param unit The time unit of the timeout argument.
   * @throws CompletionException If the computation threw an exception.
   * @throws TimeoutException If the wait timed out.
   * @throws InterruptedException If the current thread was interrupted while waiting.
   */
  void join(long timeout, TimeUnit unit) throws CompletionException, TimeoutException, InterruptedException;

  /**
   * Returns a new completion that, when this completion completes normally, completes with the same value or exception
   * as the result returned after executing the given function.
   *
   * @param fn The function returning a new result.
   * @param <U> The type of the returned result's value.
   * @return A new result.
   */
  <U> AsyncResult<U> then(Supplier<? extends AsyncResult<U>> fn);

  /**
   * Returns a new result that, when this completion completes normally, completes with the same value or exception as
   * the completion returned after executing the given function on the vertx context.
   *
   * @param vertx The vertx context.
   * @param fn The function returning a new result.
   * @param <U> The type of the returned result's value.
   * @return A new result.
   */
  <U> AsyncResult<U> thenSchedule(Vertx vertx, Supplier<? extends AsyncResult<U>> fn);

  /**
   * Returns a new completion that, when this completion completes normally, completes after given action is executed.
   *
   * @param runnable Te action to perform before completing the returned {@link AsyncCompletion}.
   * @return A completion.
   */
  AsyncCompletion thenRun(Runnable runnable);

  /**
   * Returns a new completion that, when this completion completes normally, completes after the given action is
   * executed on the vertx context.
   *
   * @param vertx The vertx context.
   * @param runnable The action to execute on the vertx context before completing the returned completion.
   * @return A completion.
   */
  AsyncCompletion thenScheduleRun(Vertx vertx, Runnable runnable);

  /**
   * Returns a new completion that, when this completion completes normally, completes after the given blocking action
   * is executed on the vertx context.
   *
   * @param vertx The vertx context.
   * @param runnable The action to execute on the vertx context before completing the returned completion.
   * @return A completion.
   */
  AsyncCompletion thenScheduleBlockingRun(Vertx vertx, Runnable runnable);

  /**
   * Returns a new completion that, when this completion completes normally, completes after the given blocking action
   * is executed on the vertx executor.
   *
   * @param executor The vertx executor.
   * @param runnable The action to execute on the vertx context before completing the returned completion.
   * @return A completion.
   */
  AsyncCompletion thenScheduleBlockingRun(WorkerExecutor executor, Runnable runnable);

  /**
   * When this result completes normally, invokes the given function with the resulting value and obtain a new
   * {@link AsyncCompletion}.
   *
   * @param fn The function returning a new completion.
   * @return A completion.
   */
  AsyncCompletion thenCompose(Supplier<? extends AsyncCompletion> fn);

  /**
   * Returns a completion that, when this result completes normally, completes with the value obtained after executing
   * the supplied function.
   *
   * @param supplier The function to use to compute the value of the returned result.
   * @param <U> The function's return type.
   * @return A new result.
   */
  <U> AsyncResult<U> thenSupply(Supplier<? extends U> supplier);

  /**
   * Returns a completion that, when this result completes normally, completes with the value obtained after executing
   * the supplied function on the vertx context.
   *
   * @param vertx The vertx context.
   * @param supplier The function to use to compute the value of the returned result.
   * @param <U> The function's return type.
   * @return A new result.
   */
  <U> AsyncResult<U> thenSupply(Vertx vertx, Supplier<? extends U> supplier);

  /**
   * Returns a completion that, when this completion and the supplied result both complete normally, completes after
   * executing the supplied function with the value from the supplied result as an argument.
   *
   * @param other The other result.
   * @param consumer The function to execute.
   * @param <U> The type of the other's value.
   * @return A new result.
   */
  <U> AsyncCompletion thenConsume(AsyncResult<? extends U> other, Consumer<? super U> consumer);

  /**
   * Returns a result that, when this completion and the other result both complete normally, completes with the value
   * obtained from executing the supplied function with the value from the other result as an argument.
   *
   * @param other The other result.
   * @param fn The function to execute.
   * @param <U> The type of the other's value.
   * @param <V> The type of the value returned by the function.
   * @return A new result.
   */
  <U, V> AsyncResult<V> thenApply(AsyncResult<? extends U> other, Function<? super U, ? extends V> fn);

  /**
   * Returns a completion that completes when both this completion and the other complete normally.
   *
   * @param other The other completion.
   * @return A completion.
   */
  AsyncCompletion thenCombine(AsyncCompletion other);

  /**
   * Returns a new completion that, when this result completes exceptionally, completes after executing the supplied
   * function. Otherwise, if this result completes normally, then the returned result also completes normally with the
   * same value.
   *
   * @param consumer The function to execute.
   * @return A new result.
   */
  AsyncCompletion exceptionally(Consumer<? super Throwable> consumer);

  /**
   * Returns a new completion that completes in the same manner as this completion, after executing the given function
   * with this completion's exception (if any).
   * <p>
   * The exception supplied to the function will be {@code null} if this completion completes successfully.
   *
   * @param consumer The action to execute.
   * @return A new result.
   */
  AsyncCompletion whenComplete(Consumer<? super Throwable> consumer);

  /**
   * Returns a new result that, when this result completes either normally or exceptionally, completes with the value
   * obtained from executing the supplied function with this result's exception (if any) as an argument.
   * <p>
   * The exception supplied to the function will be {@code null} if this completion completes successfully.
   *
   * @param fn The function to execute.
   * @param <U> The type of the value returned from the function.
   * @return A new result.
   */
  <U> AsyncResult<U> handle(Function<? super Throwable, ? extends U> fn);

  /**
   * Returns a new completion that completes successfully, after executing the given function with this completion's
   * exception (if any).
   * <p>
   * The exception supplied to the function will be {@code null} if this completion completes successfully.
   *
   * @param consumer The action to execute.
   * @return A new result.
   */
  AsyncCompletion accept(Consumer<? super Throwable> consumer);

  /**
   * Returns the underlying future associated with this instance.
   *
   * Note taking action directly on this future will impact this object.
   * 
   * @return the underlying future
   */
  Future<Void> toFuture();
}
