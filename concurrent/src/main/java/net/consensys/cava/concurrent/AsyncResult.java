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

import java.util.*;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.*;
import java.util.stream.Stream;
import javax.annotation.Nullable;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;

/**
 * A result that will be available at a future time.
 *
 * @param <T> The type of the result object.
 */
public interface AsyncResult<T> {

  /**
   * Return an already completed result containing the given value.
   *
   * @param value The value.
   * @param <T> The type of the value.
   * @return A completed result.
   */
  static <T> AsyncResult<T> completed(@Nullable T value) {
    CompletableAsyncResult<T> result = new DefaultCompletableAsyncResult<>();
    result.complete(value);
    return result;
  }

  /**
   * Return an already failed result, caused by the given exception.
   *
   * @param ex The exception.
   * @param <T> The type of the value that would be available if this result hadn't completed exceptionally.
   * @return A failed result.
   */
  static <T> AsyncResult<T> exceptional(Throwable ex) {
    requireNonNull(ex);
    CompletableAsyncResult<T> result = new DefaultCompletableAsyncResult<>();
    result.completeExceptionally(ex);
    return result;
  }

  /**
   * Return an incomplete result, that can be later completed or failed.
   *
   * @param <T> The type of the value that this result will complete with.
   * @return An incomplete result.
   */
  static <T> CompletableAsyncResult<T> incomplete() {
    return new DefaultCompletableAsyncResult<>();
  }

  /**
   * Returns an {@link AsyncCompletion} that completes when all of the given results complete. If any results complete
   * exceptionally, then the resulting completion also completes exceptionally.
   *
   * @param rs The results to combine.
   * @return A completion.
   */
  static AsyncCompletion allOf(AsyncResult<?>... rs) {
    return allOf(Arrays.stream(rs));
  }

  /**
   * Returns an {@link AsyncCompletion} that completes when all of the given results complete. If any results complete
   * exceptionally, then the resulting completion also completes exceptionally.
   *
   * @param rs The results to combine.
   * @return A completion.
   */
  static AsyncCompletion allOf(Collection<? extends AsyncResult<?>> rs) {
    return allOf(rs.stream());
  }

  /**
   * Returns an {@link AsyncCompletion} that completes when all of the given results complete. If any results complete
   * exceptionally, then the resulting completion also completes exceptionally.
   *
   * @param rs The results to combine.
   * @return A completion.
   */
  static AsyncCompletion allOf(Stream<? extends AsyncResult<?>> rs) {
    @SuppressWarnings("rawtypes")
    java.util.concurrent.CompletableFuture[] completableFutures = rs.map(result -> {
      java.util.concurrent.CompletableFuture<Void> javaFuture = new java.util.concurrent.CompletableFuture<>();
      result.whenComplete((v, ex) -> {
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
   * Returns a result that completes when all of the given results complete. If any results complete exceptionally, then
   * the resulting completion also completes exceptionally.
   *
   * @param <T> The type of the values that this result will complete with.
   * @param rs The results to combine.
   * @return A new result.
   */
  static <T> AsyncResult<List<T>> combine(Collection<? extends AsyncResult<? extends T>> rs) {
    return combine(rs.stream());
  }

  /**
   * Returns a result that completes when all of the given results complete. If any results complete exceptionally, then
   * the resulting completion also completes exceptionally.
   *
   * @param <T> The type of the values that this result will complete with.
   * @param rs The results to combine.
   * @return A new result.
   */
  static <T> AsyncResult<List<T>> combine(Stream<? extends AsyncResult<? extends T>> rs) {
    Stream<AsyncResult<List<T>>> ls = rs.map(r -> r.thenApply(Collections::singletonList));
    return ls.reduce(AsyncResult.completed(new ArrayList<>()), (r1, r2) -> r1.thenCombine(r2, (l1, l2) -> {
      l1.addAll(l2);
      return l1;
    }));
  }

  /**
   * Returns a result that, after the given function executes on a vertx context and returns a result, completes when
   * the returned result completes, with the same value or exception.
   *
   * <p>
   * Note that the given function is run directly on the context and should not block.
   *
   * @param vertx The vertx context.
   * @param fn The function returning a result.
   * @param <T> The type of the returned result's value.
   * @return A new result.
   */
  static <T> AsyncResult<T> runOnContext(Vertx vertx, Supplier<? extends AsyncResult<T>> fn) {
    requireNonNull(fn);
    CompletableAsyncResult<T> asyncResult = AsyncResult.incomplete();
    vertx.runOnContext(ev -> {
      try {
        fn.get().whenComplete((u, ex2) -> {
          if (ex2 == null) {
            try {
              asyncResult.complete(u);
            } catch (Throwable ex3) {
              asyncResult.completeExceptionally(ex3);
            }
          } else {
            asyncResult.completeExceptionally(ex2);
          }
        });
      } catch (Throwable ex1) {
        asyncResult.completeExceptionally(ex1);
      }
    });
    return asyncResult;
  }

  /**
   * Returns a result that, after the given blocking function executes asynchronously on
   * {@link ForkJoinPool#commonPool()} and returns a result, completes when the returned result completes, with the same
   * value or exception.
   *
   * @param fn The function returning a result.
   * @param <T> The type of the returned result's value.
   * @return A new result.
   */
  static <T> AsyncResult<T> executeBlocking(Supplier<T> fn) {
    requireNonNull(fn);
    CompletableAsyncResult<T> asyncResult = AsyncResult.incomplete();
    ForkJoinPool.commonPool().execute(() -> {
      try {
        asyncResult.complete(fn.get());
      } catch (Throwable ex) {
        asyncResult.completeExceptionally(ex);
      }
    });
    return asyncResult;
  }

  /**
   * Returns a result that, after the given blocking function executes asynchronously on an {@link Executor} and returns
   * a result, completes when the returned result completes, with the same value or exception.
   *
   * @param executor The executor.
   * @param fn The function returning a result.
   * @param <T> The type of the returned result's value.
   * @return A new result.
   */
  static <T> AsyncResult<T> executeBlocking(Executor executor, Supplier<T> fn) {
    requireNonNull(fn);
    CompletableAsyncResult<T> asyncResult = AsyncResult.incomplete();
    executor.execute(() -> {
      try {
        asyncResult.complete(fn.get());
      } catch (Throwable ex) {
        asyncResult.completeExceptionally(ex);
      }
    });
    return asyncResult;
  }

  /**
   * Returns a result that, after the given blocking function executes asynchronously on a vertx context and returns a
   * result, completes when the returned result completes, with the same value or exception.
   *
   * @param vertx The vertx context.
   * @param fn The function returning a result.
   * @param <T> The type of the returned result's value.
   * @return A new result.
   */
  static <T> AsyncResult<T> executeBlocking(Vertx vertx, Supplier<T> fn) {
    requireNonNull(fn);
    CompletableAsyncResult<T> asyncResult = AsyncResult.incomplete();
    vertx.<T>executeBlocking(future -> future.complete(fn.get()), false, res -> {
      if (res.succeeded()) {
        asyncResult.complete(res.result());
      } else {
        asyncResult.completeExceptionally(res.cause());
      }
    });
    return asyncResult;
  }

  /**
   * Returns a result that, after the given blocking function executes asynchronously on a vertx executor and returns a
   * result, completes when the returned result completes, with the same value or exception.
   *
   * @param executor A vertx executor.
   * @param fn The function returning a result.
   * @param <T> The type of the returned result's value.
   * @return A new result.
   */
  static <T> AsyncResult<T> executeBlocking(WorkerExecutor executor, Supplier<T> fn) {
    requireNonNull(fn);
    CompletableAsyncResult<T> asyncResult = AsyncResult.incomplete();
    executor.<T>executeBlocking(future -> future.complete(fn.get()), false, res -> {
      if (res.succeeded()) {
        asyncResult.complete(res.result());
      } else {
        asyncResult.completeExceptionally(res.cause());
      }
    });
    return asyncResult;
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
   * @return {@code true} if this result transitioned to a cancelled state.
   */
  boolean cancel();

  /**
   * Returns {@code true} if this task was cancelled before it completed normally.
   *
   * @return {@code true} if completed.
   */
  boolean isCancelled();

  /**
   * Waits if necessary for the computation to complete, and then retrieves its result.
   *
   * @return The computed result.
   * @throws CompletionException If the computation threw an exception.
   * @throws InterruptedException If the current thread was interrupted while waiting.
   */
  @Nullable
  T get() throws CompletionException, InterruptedException;

  /**
   * Waits if necessary for at most the given time for the computation to complete, and then retrieves its result.
   *
   * @param timeout The maximum time to wait.
   * @param unit The time unit of the timeout argument.
   * @return The computed result.
   * @throws CompletionException If the computation threw an exception.
   * @throws TimeoutException If the wait timed out.
   * @throws InterruptedException If the current thread was interrupted while waiting.
   */
  @Nullable
  T get(long timeout, TimeUnit unit) throws CompletionException, TimeoutException, InterruptedException;

  /**
   * Returns a new result that, when this result completes normally, completes with the same value or exception as the
   * result returned after executing the given function with this results value as an argument.
   *
   * @param fn The function returning a new result.
   * @param <U> The type of the returned result's value.
   * @return A new result.
   */
  <U> AsyncResult<U> then(Function<? super T, ? extends AsyncResult<U>> fn);

  /**
   * Returns a new result that, when this result completes normally, completes with the same value or exception as the
   * completion returned after executing the given function on the vertx context with this results value as an argument.
   *
   * @param vertx The vertx context.
   * @param fn The function returning a new result.
   * @param <U> The type of the returned result's value.
   * @return A new result.
   */
  <U> AsyncResult<U> thenSchedule(Vertx vertx, Function<? super T, ? extends AsyncResult<U>> fn);

  /**
   * When this result completes normally, invokes the given function with the resulting value and obtains a new
   * {@link AsyncCompletion}.
   *
   * @param fn The function returning a new completion.
   * @return A completion.
   */
  AsyncCompletion thenCompose(Function<? super T, ? extends AsyncCompletion> fn);

  /**
   * Returns a new completion that, when this result completes normally, completes after given action is executed.
   *
   * @param runnable The action to execute before completing the returned completion.
   * @return A completion.
   */
  AsyncCompletion thenRun(Runnable runnable);

  /**
   * Returns a new completion that, when this result completes normally, completes after the given action is executed on
   * the vertx context.
   *
   * @param vertx The vertx context.
   * @param runnable The action to execute on the vertx context before completing the returned completion.
   * @return A completion.
   */
  AsyncCompletion thenScheduleRun(Vertx vertx, Runnable runnable);

  /**
   * Returns a new completion that, when this result completes normally, completes after the given blocking action is
   * executed on the vertx context.
   *
   * @param vertx The vertx context.
   * @param runnable The action to execute on the vertx context before completing the returned completion.
   * @return A completion.
   */
  AsyncCompletion thenScheduleBlockingRun(Vertx vertx, Runnable runnable);

  /**
   * Returns a new completion that, when this result completes normally, completes after the given blocking action is
   * executed on the vertx executor.
   *
   * @param executor The vertx executor.
   * @param runnable The action to execute on the vertx context before completing the returned completion.
   * @return A completion.
   */
  AsyncCompletion thenScheduleBlockingRun(WorkerExecutor executor, Runnable runnable);

  /**
   * Returns a result that, when this result completes normally, completes with the value obtained from executing the
   * supplied function with this result's value as an argument.
   *
   * @param fn The function to use to compute the value of the returned result.
   * @param <U> The function's return type.
   * @return A new result.
   */
  <U> AsyncResult<U> thenApply(Function<? super T, ? extends U> fn);

  /**
   * Returns a result that, when this result completes normally, completes with the value obtained from executing the
   * supplied function on the vertx context with this result's value as an argument.
   *
   * @param vertx The vertx context.
   * @param fn The function to use to compute the value of the returned result.
   * @param <U> The function's return type.
   * @return A new result.
   */
  <U> AsyncResult<U> thenScheduleApply(Vertx vertx, Function<? super T, ? extends U> fn);

  /**
   * Returns a result that, when this result completes normally, completes with the value obtained from executing the
   * supplied blocking function on the vertx context with this result's value as an argument.
   *
   * @param vertx The vertx context.
   * @param fn The function to use to compute the value of the returned result.
   * @param <U> The function's return type.
   * @return A new result.
   */
  <U> AsyncResult<U> thenScheduleBlockingApply(Vertx vertx, Function<? super T, ? extends U> fn);

  /**
   * Returns a result that, when this result completes normally, completes with the value obtained from executing the
   * supplied blocking function on the vertx executor with this result's value as an argument.
   *
   * @param executor The vertx executor.
   * @param fn The function to use to compute the value of the returned result.
   * @param <U> The function's return type.
   * @return A new result.
   */
  <U> AsyncResult<U> thenScheduleBlockingApply(WorkerExecutor executor, Function<? super T, ? extends U> fn);

  /**
   * Returns a completion that, when this result completes normally, completes after executing the supplied consumer
   * with this result's value as an argument.
   *
   * @param consumer The consumer for the value of this result.
   * @return A completion.
   */
  AsyncCompletion thenAccept(Consumer<? super T> consumer);

  /**
   * Returns a completion that, when this result and the other result both complete normally, completes after executing
   * the supplied consumer with both this result's value and the value from the other result as arguments.
   *
   * @param other The other result.
   * @param consumer The consumer for both values.
   * @param <U> The type of the other's value.
   * @return A completion.
   */
  <U> AsyncCompletion thenAcceptBoth(AsyncResult<? extends U> other, BiConsumer<? super T, ? super U> consumer);

  /**
   * Returns a result that, when this result and the other result both complete normally, completes with the value
   * obtained from executing the supplied function with both this result's value and the value from the other result as
   * arguments.
   *
   * @param other The other result.
   * @param fn The function to execute.
   * @param <U> The type of the other's value.
   * @param <V> The type of the value returned by the function.
   * @return A new result.
   */
  <U, V> AsyncResult<V> thenCombine(AsyncResult<? extends U> other, BiFunction<? super T, ? super U, ? extends V> fn);

  /**
   * Returns a new result that, when this result completes exceptionally, completes with the value obtained from
   * executing the supplied function with this result's exception as an argument. Otherwise, if this result completes
   * normally, then the returned result also completes normally with the same value.
   *
   * @param fn The function to execute.
   * @return A new result.
   */
  AsyncResult<T> exceptionally(Function<Throwable, ? extends T> fn);

  /**
   * Returns a new result that completes with the same value or exception as this result, after executing the given
   * action with this result's value or exception.
   * <p>
   * Either the value or the exception supplied to the action will be {@code null}.
   *
   * @param action The action to execute.
   * @return A new result.
   */
  AsyncResult<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);

  /**
   * Returns a new result that, when this result completes either normally or exceptionally, completes with the value
   * obtained from executing the supplied function with this result's value and exception as arguments.
   * <p>
   * Either the value or the exception supplied to the function will be {@code null}.
   *
   * @param fn The function to execute.
   * @param <U> The type of the value returned from the function.
   * @return A new result.
   */
  <U> AsyncResult<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);

  /**
   * Returns a new completion that, when this result completes either normally or exceptionally, completes after
   * executing the supplied function with this result's value and exception as arguments.
   * <p>
   * Either the value or the exception supplied to the function will be {@code null}.
   *
   * @param consumer The consumer to execute.
   * @return A completion.
   */
  AsyncCompletion accept(BiConsumer<? super T, Throwable> consumer);
}
