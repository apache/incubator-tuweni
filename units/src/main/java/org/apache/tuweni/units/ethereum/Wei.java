// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.units.ethereum;

import org.apache.tuweni.units.bigints.BaseUInt256Value;
import org.apache.tuweni.units.bigints.UInt256;

import java.math.BigInteger;

/** A unit measure of Wei as used by the Ethereum VM. */
public final class Wei extends BaseUInt256Value<Wei> {

  private static final int MAX_CONSTANT = 64;
  private static final BigInteger BI_MAX_CONSTANT = BigInteger.valueOf(MAX_CONSTANT);
  private static final UInt256 UINT256_MAX_CONSTANT = UInt256.valueOf(MAX_CONSTANT);
  private static Wei CONSTANTS[] = new Wei[MAX_CONSTANT + 1];

  static {
    CONSTANTS[0] = new Wei(UInt256.ZERO);
    for (int i = 1; i <= MAX_CONSTANT; ++i) {
      CONSTANTS[i] = new Wei(i);
    }
  }

  public static final Wei ZERO = Wei.valueOf(0);

  private Wei(UInt256 bytes) {
    super(bytes, Wei::new);
  }

  /**
   * Return a {@link Wei} containing the specified value from an ETH
   *
   * @param ethValue the value in eth
   * @return A {@link Wei} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static Wei fromEth(long ethValue) {
    return valueOf(ethValue * (long) Math.pow(10, 18));
  }

  /**
   * Return a {@link Wei} containing the specified value.
   *
   * @param value The value to create a {@link Wei} for.
   * @return A {@link Wei} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static Wei valueOf(UInt256 value) {
    if (value.compareTo(UINT256_MAX_CONSTANT) <= 0) {
      return CONSTANTS[value.intValue()];
    }
    return new Wei(value);
  }

  private Wei(long value) {
    super(value, Wei::new);
  }

  /**
   * Return a {@link Wei} containing the specified value.
   *
   * @param value The value to create a {@link Wei} for.
   * @return A {@link Wei} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static Wei valueOf(long value) {
    if (value < 0) {
      throw new IllegalArgumentException("Argument must be positive");
    }
    if (value <= MAX_CONSTANT) {
      return CONSTANTS[(int) value];
    }
    return new Wei(value);
  }

  private Wei(BigInteger value) {
    super(value, Wei::new);
  }

  /**
   * Return a {@link Wei} containing the specified value.
   *
   * @param value The value to create a {@link Wei} for.
   * @return A {@link Wei} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static Wei valueOf(BigInteger value) {
    if (value.signum() < 0) {
      throw new IllegalArgumentException("Argument must be positive");
    }
    if (value.compareTo(BI_MAX_CONSTANT) <= 0) {
      return CONSTANTS[value.intValue()];
    }
    return new Wei(value);
  }
}
