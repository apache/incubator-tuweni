// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import javax.annotation.Nullable;

import com.google.errorprone.annotations.FormatMethod;

class Checks {

  static void checkNotNull(@Nullable Object object) {
    if (object == null) {
      throw new NullPointerException("argument cannot be null");
    }
  }

  static void checkElementIndex(int index, int size) {
    if (index < 0 || index >= size) {
      throw new IndexOutOfBoundsException("index is out of bounds");
    }
  }

  @FormatMethod
  static void checkArgument(boolean condition, String message, Object... args) {
    if (!condition) {
      throw new IllegalArgumentException(String.format(message, args));
    }
  }
}
