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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;

final class DefaultCompletableAsyncCompletion implements CompletableAsyncCompletion {

  private final CompletableFuture<Void> future;

  DefaultCompletableAsyncCompletion() {
    this(new CompletableFuture<>());
  }

  DefaultCompletableAsyncCompletion(CompletableFuture<Void> future) {
    this.future = future;
  }

  @Override
  public boolean complete() {
    return future.complete(null);
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
  public void join() throws CompletionException, InterruptedException {
    try {
      join(10, TimeUnit.SECONDS);
    } catch (TimeoutException ex) {
      throw new RuntimeException("Default timeout triggered for blocking call to AsyncCompletion::join()", ex);
    }
  }

  @Override
  public void join(long timeout, TimeUnit unit) throws CompletionException, TimeoutException, InterruptedException {
    requireNonNull(unit);
    try {
      future.get(timeout, unit);
    } catch (ExecutionException ex) {
      throw new CompletionException(ex.getMessage(), ex.getCause());
    }
  }

  @Override
  public <U> AsyncResult<U> then(Supplier<? extends AsyncResult<U>> fn) {
    requireNonNull(fn);
    CompletableAsyncResult<U> asyncResult = AsyncResult.incomplete();
    future.whenComplete((v, ex1) -> {
      if (ex1 == null) {
        try {
          fn.get().whenComplete((u, ex3) -> {
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
  public <U> AsyncResult<U> thenSchedule(Vertx vertx, Supplier<? extends AsyncResult<U>> fn) {
    requireNonNull(vertx);
    requireNonNull(fn);
    CompletableAsyncResult<U> asyncResult = AsyncResult.incomplete();
    future.whenComplete((v, ex1) -> {
      if (ex1 == null) {
        try {
          vertx.runOnContext(ev -> {
            try {
              fn.get().whenComplete((u, ex4) -> {
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
  public AsyncCompletion thenCompose(Supplier<? extends AsyncCompletion> fn) {
    requireNonNull(fn);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    future.whenComplete((v, ex1) -> {
      if (ex1 == null) {
        try {
          fn.get().whenComplete(ex3 -> {
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
  public <U> AsyncResult<U> thenSupply(Supplier<? extends U> supplier) {
    requireNonNull(supplier);
    return new DefaultCompletableAsyncResult<>(future.thenApply(v -> supplier.get()));
  }

  @Override
  public <U> AsyncResult<U> thenSupply(Vertx vertx, Supplier<? extends U> supplier) {
    requireNonNull(vertx);
    requireNonNull(supplier);
    CompletableAsyncResult<U> completion = AsyncResult.incomplete();
    future.whenComplete((t, ex1) -> {
      if (ex1 == null) {
        try {
          vertx.runOnContext(ev -> {
            try {
              completion.complete(supplier.get());
            } catch (Throwable ex3) {
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
  public <U> AsyncCompletion thenConsume(AsyncResult<? extends U> other, Consumer<? super U> consumer) {
    requireNonNull(other);
    requireNonNull(consumer);
    return new DefaultCompletableAsyncCompletion(future.thenAccept(v -> other.thenAccept(consumer)));
  }

  @Override
  public <U, V> AsyncResult<V> thenApply(AsyncResult<? extends U> other, Function<? super U, ? extends V> fn) {
    requireNonNull(other);
    requireNonNull(fn);
    CompletableAsyncResult<V> asyncResult = AsyncResult.incomplete();
    future.whenComplete((v, ex1) -> {
      if (ex1 == null) {
        try {
          other.whenComplete((u, ex3) -> {
            if (ex3 == null) {
              asyncResult.complete(fn.apply(u));
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
  public AsyncCompletion thenCombine(AsyncCompletion other) {
    requireNonNull(other);
    CompletableAsyncCompletion completion = AsyncCompletion.incomplete();
    future.whenComplete((v, ex1) -> {
      if (ex1 == null) {
        try {
          other.whenComplete(ex3 -> {
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
  public AsyncCompletion exceptionally(Consumer<? super Throwable> consumer) {
    requireNonNull(consumer);
    return new DefaultCompletableAsyncCompletion(future.exceptionally(ex -> {
      consumer.accept(ex);
      return null;
    }));
  }

  @Override
  public AsyncCompletion whenComplete(Consumer<? super Throwable> consumer) {
    requireNonNull(consumer);
    return new DefaultCompletableAsyncCompletion(future.whenComplete((v, ex) -> consumer.accept(ex)));
  }

  @Override
  public <U> AsyncResult<U> handle(Function<? super Throwable, ? extends U> fn) {
    requireNonNull(fn);
    return new DefaultCompletableAsyncResult<>(future.handle((v, ex) -> fn.apply(ex)));
  }

  @Override
  public AsyncCompletion accept(Consumer<? super Throwable> consumer) {
    requireNonNull(consumer);
    return new DefaultCompletableAsyncCompletion(future.handle((v, ex) -> {
      consumer.accept(ex);
      return null;
    }));
  }

  @Override
  public CompletableFuture<Void> toFuture() {
    return future;
  }
}
