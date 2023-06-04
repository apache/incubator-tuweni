// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.concurrent;

import javax.annotation.Nullable;

/**
 * An {@link AsyncResult} that can be later completed successfully with a provided value, or
 * completed with an exception.
 *
 * @param <T> The type of the value returned by this result.
 */
public interface CompletableAsyncResult<T> extends AsyncResult<T> {

  /**
   * Complete this result with the given value.
   *
   * @param value The value to complete this result with.
   * @return {@code true} if this invocation caused this result to transition to a completed state,
   *     else {@code false}.
   */
  boolean complete(@Nullable T value);

  /**
   * Complete this result with the given exception.
   *
   * @param ex The exception to complete this result with.
   * @return {@code true} if this invocation caused this result to transition to a completed state,
   *     else {@code false}.
   */
  boolean completeExceptionally(Throwable ex);
}
