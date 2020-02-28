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

import java.util.concurrent.CompletableFuture;

/**
 * An {@link AsyncCompletion} that can later be completed successfully or with a provided exception.
 */
public interface CompletableAsyncCompletion extends AsyncCompletion {

  /**
   * Complete this completion.
   *
   * @return {@code true} if this invocation caused this completion to transition to a completed state, else
   *         {@code false}.
   */
  boolean complete();

  /**
   * Complete this completion with the given exception.
   *
   * @param ex The exception to complete this result with.
   * @return {@code true} if this invocation caused this completion to transition to a completed state, else
   *         {@code false}.
   */
  boolean completeExceptionally(Throwable ex);

  /**
   * Returns the underlying completable future associated with this instance.
   *
   * Note taking action directly on this future will impact this object.
   */
  @Override
  CompletableFuture<Void> toFuture();
}
