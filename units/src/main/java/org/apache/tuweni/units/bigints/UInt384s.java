// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.units.bigints;

/** Static utility methods on UInt384 values. */
public final class UInt384s {
  private UInt384s() {}

  /**
   * Returns the maximum of two UInt384 values.
   *
   * @param v1 The first value.
   * @param v2 The second value.
   * @return The maximum of {@code v1} and {@code v2}.
   * @param <T> The concrete type of the two values.
   */
  public static <T extends UInt384Value<T>> T max(T v1, T v2) {
    return (v1.compareTo(v2)) >= 0 ? v1 : v2;
  }

  /**
   * Returns the minimum of two UInt384 values.
   *
   * @param v1 The first value.
   * @param v2 The second value.
   * @return The minimum of {@code v1} and {@code v2}.
   * @param <T> The concrete type of the two values.
   */
  public static <T extends UInt384Value<T>> T min(T v1, T v2) {
    return (v1.compareTo(v2)) < 0 ? v1 : v2;
  }
}
