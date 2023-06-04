// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.concurrent;

import java.util.concurrent.CompletableFuture;

/**
 * An {@link AsyncCompletion} that can later be completed successfully or with a provided exception.
 */
public interface CompletableAsyncCompletion extends AsyncCompletion {

  /**
   * Complete this completion.
   *
   * @return {@code true} if this invocation caused this completion to transition to a completed
   *     state, else {@code false}.
   */
  boolean complete();

  /**
   * Complete this completion with the given exception.
   *
   * @param ex The exception to complete this result with.
   * @return {@code true} if this invocation caused this completion to transition to a completed
   *     state, else {@code false}.
   */
  boolean completeExceptionally(Throwable ex);

  /**
   * Returns the underlying completable future associated with this instance.
   *
   * <p>Note taking action directly on this future will impact this object.
   */
  @Override
  CompletableFuture<Void> toFuture();
}
