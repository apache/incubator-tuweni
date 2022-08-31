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
package org.apache.tuweni.units.ethereum;

import org.apache.tuweni.units.bigints.BaseUInt256Value;
import org.apache.tuweni.units.bigints.UInt256;

import java.math.BigInteger;

/**
 * A unit measure of Gas as used by the Ethereum VM.
 */
public final class Gas extends BaseUInt256Value<Gas> {

  private final static int MAX_CONSTANT = 64;
  private final static BigInteger BI_MAX_CONSTANT = BigInteger.valueOf(MAX_CONSTANT);
  private final static UInt256 UINT256_MAX_CONSTANT = UInt256.valueOf(MAX_CONSTANT);
  private static final Gas[] CONSTANTS = new Gas[MAX_CONSTANT + 1];
  static {
    CONSTANTS[0] = new Gas(UInt256.ZERO);
    for (int i = 1; i <= MAX_CONSTANT; ++i) {
      CONSTANTS[i] = new Gas(i);
    }
  }

  public final static Gas ZERO = Gas.valueOf(0);
  public final static Gas MAX = Gas.valueOf(Long.MAX_VALUE);

  private Gas(UInt256 bytes) {
    super(bytes, Gas::new);
  }

  private Gas(long value) {
    super(value, Gas::new);
  }

  /**
   * Return a {@link Gas} containing the specified value.
   *
   * @param value The value to create a {@link Gas} for.
   * @return A {@link Gas} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static Gas valueOf(UInt256 value) {
    if (value.compareTo(UINT256_MAX_CONSTANT) <= 0) {
      return CONSTANTS[value.intValue()];
    }
    return new Gas(value);
  }

  /**
   * Return a {@link Gas} containing the specified value.
   *
   * @param value The value to create a {@link Gas} for.
   * @return A {@link Gas} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static Gas valueOf(long value) {
    if (value < 0) {
      throw new IllegalArgumentException("Argument must be positive");
    }
    if (value <= MAX_CONSTANT) {
      return CONSTANTS[(int) value];
    }
    return new Gas(value);
  }

  /**
   * Return a {@link Gas} containing the specified value.
   *
   * @param value The value to create a {@link Gas} for.
   * @return A {@link Gas} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static Gas valueOf(BigInteger value) {
    if (value.signum() < 0) {
      throw new IllegalArgumentException("Argument must be positive");
    }
    if (value.compareTo(BI_MAX_CONSTANT) <= 0) {
      return CONSTANTS[value.intValue()];
    }
    return new Gas(UInt256.valueOf(value));
  }

  /**
   * Provides the minimum value between 2 gas objects.
   * 
   * @param one a gas object
   * @param two another gas object
   * @return the minimum between the 2 gas objects
   */
  public static Gas minimum(Gas one, Gas two) {
    return one.compareTo(two) <= 0 ? one : two;
  }

  /**
   * The price of this amount of gas given the provided price per unit of gas.
   *
   * @param gasPrice The price per unit of gas.
   * @return The price of this amount of gas for a per unit of gas price of {@code gasPrice}.
   */
  public Wei priceFor(Wei gasPrice) {
    return Wei.valueOf(gasPrice.toUInt256().multiply(this.toUInt256()));
  }

  /**
   * Returns true if the gas value is past the maximum allowed gas, 2^63 -1
   * 
   * @return true if gas is past allowed maximum
   */
  public boolean tooHigh() {
    return this.greaterThan(MAX);
  }
}
