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

import javax.annotation.Nullable;

/**
 * An {@link AsyncResult} that can be later completed successfully with a provided value, or completed with an
 * exception.
 *
 * @param <T> The type of the value returned by this result.
 */
public interface CompletableAsyncResult<T> extends AsyncResult<T> {

  /**
   * Complete this result with the given value.
   *
   * @param value The value to complete this result with.
   * @return {@code true} if this invocation caused this result to transition to a completed state, else {@code false}.
   */
  boolean complete(@Nullable T value);

  /**
   * Complete this result with the given exception.
   *
   * @param ex The exception to complete this result with.
   * @return {@code true} if this invocation caused this result to transition to a completed state, else {@code false}.
   */
  boolean completeExceptionally(Throwable ex);
}
