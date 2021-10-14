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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;

final class DefaultCompletableAsyncResult<T> implements CompletableAsyncResult<T> {

  private final CompletableFuture<T> future;

  DefaultCompletableAsyncResult() {
    this(new CompletableFuture<>());
  }

  DefaultCompletableAsyncResult(CompletableFuture<T> future) {
    this.future = future;
  }

  @Override
  public boolean complete(@Nullable T value) {
    return future.complete(value);
  }

  @Override
  public boolean completeExceptionally(Throwable ex) {
    return future.completeExceptionally(ex);
  }

  @Override
  public boolean cancel() {
    return future.cancel(false);
  }

  @Override
  public boolean isDone() {
    return future.isDone();
  }

  @Override
  public boolean isCompletedExceptionally() {
    return future.isCompletedExceptionally();
  }

  @Override
  public boolean isCancelled() {
    return future.isCancelled();
  }

  @Override
  @Nullable
  public T get() throws CompletionException, InterruptedException {
    try {
      return get(10, TimeUnit.SECONDS);
    } catch (TimeoutException ex) {
      throw new RuntimeException("Default timeout triggered for blocking call to AsyncResult::get()", ex);
    }
  }

  @Override
  @Nullable
  public T get(long timeout, TimeUnit unit) throws CompletionException, TimeoutException, InterruptedException {
    requireNonNull(unit);
    try {
      return future.get(timeout, unit);
    } catch (ExecutionException ex) {
      throw new CompletionException(ex.getMessage(), ex.getCause());
    }
  }

  @Override
  public <U> AsyncResult<U> then(Function<? super T, ? extends AsyncResult<U>> fn) {
    requireNonNull(fn);
    CompletableAsyncResult<U> asyncResult = AsyncResult.incomplete();
    future.whenComplete((t, ex1) -> {
      if (ex1 == null) {
        try {
          fn.apply(t).whenComplete((u, ex3) -> {
            if (ex3 == null) {
              asyncResult.complete(u);
            } else {
              asyncResult.completeExceptionally(ex3);
            }
          });
        } catch (Throwable ex2) {
          asyncResult.completeExceptionally(ex2);
        }
      } else {
        asyncResult.completeExceptionally(ex1);
      }
    });
    return asyncResult;
  }

  @Override
  public <U> AsyncResult<U> thenSchedule(Vertx vertx, Function<? super T, ? extends AsyncResult<U>> fn) {
    requireNonNull(vertx);
    requireNonNull(fn);
    CompletableAsyncResult<U> asyncResult = AsyncResult.incomplete();
    future.whenComplete((t, ex1) -> {
      if (ex1 == null) {
        try {
          vertx.runOnContext(ev -> {
            try {
              fn.apply(t).whenComplete((u, ex4) -> {
                if (ex4 == null) {
                  asyncResult.complete(u);
                } else {
                  asyncResult.completeExceptionally(ex4);
                }
              });
            } catch (Throwable ex3) {
              asyncResult.completeExceptionally(ex3);
            }
          });
        } catch (Throwable ex2) {
          asyncResult.completeExceptionally(ex2);
        }
      } else {
        asyncResult.completeExceptionally(ex1);
      }
    });
    return asyncResult;
  }

  @Override
  public AsyncCompletion thenCompose(Function<? super T, ? extends AsyncCompletion> fn) {
    requireNonNull(fn);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    future.whenComplete((t, ex1) -> {
      if (ex1 == null) {
        try {
          fn.apply(t).whenComplete(ex3 -> {
            if (ex3 == null) {
              completion.complete();
            } else {
              completion.completeExceptionally(ex3);
            }
          });
        } catch (Throwable ex2) {
          completion.completeExceptionally(ex2);
        }
      } else {
        completion.completeExceptionally(ex1);
      }
    });
    return completion;
  }

  @Override
  public AsyncCompletion thenRun(Runnable action) {
    requireNonNull(action);
    return new DefaultCompletableAsyncCompletion(future.thenRun(action));
  }

  @Override
  public AsyncCompletion thenScheduleRun(Vertx vertx, Runnable runnable) {
    requireNonNull(vertx);
    requireNonNull(runnable);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    future.whenComplete((t, ex1) -> {
      if (ex1 == null) {
        try {
          vertx.runOnContext(ev -> {
            try {
              runnable.run();
            } catch (Throwable ex3) {
              completion.completeExceptionally(ex3);
              return;
            }
            completion.complete();
          });
        } catch (Throwable ex2) {
          completion.completeExceptionally(ex2);
        }
      } else {
        completion.completeExceptionally(ex1);
      }
    });
    return completion;
  }

  @Override
  public AsyncCompletion thenScheduleBlockingRun(Vertx vertx, Runnable runnable) {
    requireNonNull(vertx);
    requireNonNull(runnable);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    future.whenComplete((t, ex1) -> {
      if (ex1 == null) {
        try {
          vertx.executeBlocking(vertxFuture -> {
            runnable.run();
            vertxFuture.complete(null);
          }, false, res -> {
            if (res.succeeded()) {
              completion.complete();
            } else {
              completion.completeExceptionally(res.cause());
            }
          });
        } catch (Throwable ex2) {
          completion.completeExceptionally(ex2);
        }
      } else {
        completion.completeExceptionally(ex1);
      }
    });
    return completion;
  }

  @Override
  public AsyncCompletion thenScheduleBlockingRun(WorkerExecutor executor, Runnable runnable) {
    requireNonNull(executor);
    requireNonNull(runnable);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    future.whenComplete((t, ex1) -> {
      if (ex1 == null) {
        try {
          executor.executeBlocking(vertxFuture -> {
            runnable.run();
            vertxFuture.complete(null);
          }, false, res -> {
            if (res.succeeded()) {
              completion.complete();
            } else {
              completion.completeExceptionally(res.cause());
            }
          });
        } catch (Throwable ex2) {
          completion.completeExceptionally(ex2);
        }
      } else {
        completion.completeExceptionally(ex1);
      }
    });
    return completion;
  }

  @Override
  public <U> AsyncResult<U> thenApply(Function<? super T, ? extends U> fn) {
    requireNonNull(fn);
    return new DefaultCompletableAsyncResult<>(future.thenApply(fn));
  }

  @Override
  public <U> AsyncResult<U> thenScheduleApply(Vertx vertx, Function<? super T, ? extends U> fn) {
    requireNonNull(vertx);
    requireNonNull(fn);
    CompletableAsyncResult<U> asyncResult = AsyncResult.incomplete();
    future.whenComplete((t, ex1) -> {
      if (ex1 == null) {
        try {
          vertx.runOnContext(ev -> {
            try {
              asyncResult.complete(fn.apply(t));
            } catch (Throwable ex3) {
              asyncResult.completeExceptionally(ex3);
            }
          });
        } catch (Throwable ex2) {
          asyncResult.completeExceptionally(ex2);
        }
      } else {
        asyncResult.completeExceptionally(ex1);
      }
    });
    return asyncResult;
  }

  @Override
  public <U> AsyncResult<U> thenScheduleBlockingApply(Vertx vertx, Function<? super T, ? extends U> fn) {
    requireNonNull(vertx);
    requireNonNull(fn);
    CompletableAsyncResult<U> asyncResult = AsyncResult.incomplete();
    future.whenComplete((t, ex1) -> {
      if (ex1 == null) {
        try {
          vertx.<U>executeBlocking(vertxFuture -> vertxFuture.complete(fn.apply(t)), false, res -> {
            if (res.succeeded()) {
              asyncResult.complete(res.result());
            } else {
              asyncResult.completeExceptionally(res.cause());
            }
          });
        } catch (Throwable ex2) {
          asyncResult.completeExceptionally(ex2);
        }
      } else {
        asyncResult.completeExceptionally(ex1);
      }
    });
    return asyncResult;
  }

  @Override
  public <U> AsyncResult<U> thenScheduleBlockingApply(WorkerExecutor executor, Function<? super T, ? extends U> fn) {
    requireNonNull(executor);
    requireNonNull(fn);
    CompletableAsyncResult<U> asyncResult = AsyncResult.incomplete();
    future.whenComplete((t, ex1) -> {
      if (ex1 == null) {
        try {
          executor.<U>executeBlocking(vertxFuture -> vertxFuture.complete(fn.apply(t)), false, res -> {
            if (res.succeeded()) {
              asyncResult.complete(res.result());
            } else {
              asyncResult.completeExceptionally(res.cause());
            }
          });
        } catch (Throwable ex2) {
          asyncResult.completeExceptionally(ex2);
        }
      } else {
        asyncResult.completeExceptionally(ex1);
      }
    });
    return asyncResult;
  }

  @Override
  public AsyncCompletion thenAccept(Consumer<? super T> consumer) {
    requireNonNull(consumer);
    return new DefaultCompletableAsyncCompletion(future.thenAccept(consumer));
  }

  @Override
  public <U> AsyncCompletion thenAcceptBoth(AsyncResult<? extends U> other, BiConsumer<? super T, ? super U> consumer) {
    requireNonNull(other);
    requireNonNull(consumer);
    return new DefaultCompletableAsyncCompletion(future.thenAccept(t -> other.thenAccept(u -> consumer.accept(t, u))));
  }

  @Override
  public <U, V> AsyncResult<V> thenCombine(
      AsyncResult<? extends U> other,
      BiFunction<? super T, ? super U, ? extends V> fn) {
    requireNonNull(other);
    requireNonNull(fn);
    CompletableAsyncResult<V> asyncResult = AsyncResult.incomplete();
    future.whenComplete((t, ex1) -> {
      if (ex1 == null) {
        try {
          other.whenComplete((u, ex3) -> {
            if (ex3 == null) {
              asyncResult.complete(fn.apply(t, u));
            } else {
              asyncResult.completeExceptionally(ex3);
            }
          });
        } catch (Throwable ex2) {
          asyncResult.completeExceptionally(ex2);
        }
      } else {
        asyncResult.completeExceptionally(ex1);
      }
    });
    return asyncResult;
  }

  @Override
  public AsyncResult<T> exceptionally(Function<Throwable, ? extends T> fn) {
    requireNonNull(fn);
    return new DefaultCompletableAsyncResult<>(future.exceptionally(fn));
  }

  @Override
  public AsyncResult<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
    requireNonNull(action);
    return new DefaultCompletableAsyncResult<>(future.whenComplete(action));
  }

  @Override
  public <U> AsyncResult<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
    requireNonNull(fn);
    return new DefaultCompletableAsyncResult<>(future.handle(fn));
  }

  @Override
  public AsyncCompletion accept(BiConsumer<? super T, Throwable> consumer) {
    requireNonNull(consumer);
    return new DefaultCompletableAsyncCompletion(future.handle((t, ex) -> {
      consumer.accept(t, ex);
      return null;
    }));
  }
}
