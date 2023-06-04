// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.io;

import static java.util.Objects.requireNonNull;

import java.util.Enumeration;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Utilities for working with streams.
 */
public final class Streams {
  private Streams() {}

  /**
   * Stream an {@link Enumeration}.
   *
   * @param enumeration The enumeration.
   * @param <T> The type of objects in the enumeration.
   * @return A stream over the enumeration.
   */
  @SuppressWarnings("JdkObsolete")
  public static <T> Stream<T> enumerationStream(Enumeration<T> enumeration) {
    requireNonNull(enumeration);
    return StreamSupport.stream(new Spliterators.AbstractSpliterator<>(Long.MAX_VALUE, Spliterator.ORDERED) {
      @Override
      public boolean tryAdvance(Consumer<? super T> action) {
        if (enumeration.hasMoreElements()) {
          action.accept(enumeration.nextElement());
          return true;
        }
        return false;
      }
    }, false);
  }
}
