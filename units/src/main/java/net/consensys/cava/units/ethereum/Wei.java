/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.units.ethereum;

import static com.google.common.base.Preconditions.checkArgument;

import net.consensys.cava.units.bigints.BaseUInt256Value;
import net.consensys.cava.units.bigints.UInt256;

import java.math.BigInteger;

/**
 * A unit measure of Wei as used by the Ethereum VM.
 */
public final class Wei extends BaseUInt256Value<Wei> {

  private final static int MAX_CONSTANT = 64;
  private final static BigInteger BI_MAX_CONSTANT = BigInteger.valueOf(MAX_CONSTANT);
  private final static UInt256 UINT256_MAX_CONSTANT = UInt256.valueOf(MAX_CONSTANT);
  private static Wei CONSTANTS[] = new Wei[MAX_CONSTANT + 1];
  static {
    CONSTANTS[0] = new Wei(UInt256.ZERO);
    for (int i = 1; i <= MAX_CONSTANT; ++i) {
      CONSTANTS[i] = new Wei(i);
    }
  }

  private Wei(UInt256 bytes) {
    super(bytes, Wei::new);
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
    checkArgument(value >= 0, "Argument must be positive");
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
    checkArgument(value.signum() >= 0, "Argument must be positive");
    if (value.compareTo(BI_MAX_CONSTANT) <= 0) {
      return CONSTANTS[value.intValue()];
    }
    return new Wei(value);
  }
}
