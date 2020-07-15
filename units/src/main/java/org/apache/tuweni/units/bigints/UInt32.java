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
package org.apache.tuweni.units.bigints;

import static com.google.common.base.Preconditions.checkArgument;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.MutableBytes;

import java.math.BigInteger;

/**
 * An unsigned 32-bit precision number.
 *
 * This is a raw {@link UInt32Value} - a 32-bit precision unsigned number of no particular unit.
 */
public final class UInt32 implements UInt32Value<UInt32> {
  private final static int MAX_CONSTANT = 32;
  private static UInt32[] CONSTANTS = new UInt32[MAX_CONSTANT + 1];
  static {
    CONSTANTS[0] = new UInt32(0);
    for (int i = 1; i <= MAX_CONSTANT; ++i) {
      CONSTANTS[i] = new UInt32(i);
    }
  }

  /** The minimum value of a UInt32 */
  public final static UInt32 MIN_VALUE = valueOf(0);
  /** The maximum value of a UInt32 */
  public final static UInt32 MAX_VALUE = new UInt32(~0);
  /** The value 0 */
  public final static UInt32 ZERO = valueOf(0);
  /** The value 1 */
  public final static UInt32 ONE = valueOf(1);

  private static final BigInteger P_2_32 = BigInteger.valueOf(2).pow(32);

  private final int value;

  /**
   * Return a {@code UInt32} containing the specified value.
   *
   * @param value The value to create a {@code UInt32} for.
   * @return A {@code UInt32} containing the specified value.
   * @throws IllegalArgumentException If the value is negative.
   */
  public static UInt32 valueOf(int value) {
    checkArgument(value >= 0, "Argument must be positive");
    return create(value);
  }

  /**
   * Return a {@link UInt32} containing the specified value.
   *
   * @param value the value to create a {@link UInt32} for
   * @return a {@link UInt32} containing the specified value
   * @throws IllegalArgumentException if the value is negative or too large to be represented as a UInt32
   */
  public static UInt32 valueOf(BigInteger value) {
    checkArgument(value.signum() >= 0, "Argument must be positive");
    checkArgument(value.bitLength() <= 32, "Argument is too large to represent a UInt32");
    return create(value.intValue());
  }

  /**
   * Return a {@link UInt32} containing the value described by the specified bytes.
   *
   * @param bytes The bytes containing a {@link UInt32}.
   * @return A {@link UInt32} containing the specified value.
   * @throws IllegalArgumentException if {@code bytes.size() &gt; 8}.
   */
  public static UInt32 fromBytes(Bytes bytes) {
    checkArgument(bytes.size() <= 8, "Argument is greater than 8 bytes");
    return create(bytes.toInt());
  }

  /**
   * Parse a hexadecimal string into a {@link UInt32}.
   *
   * @param str The hexadecimal string to parse, which may or may not start with "0x". That representation may contain
   *        less than 8 bytes, in which case the result is left padded with zeros.
   * @return The value corresponding to {@code str}.
   * @throws IllegalArgumentException if {@code str} does not correspond to a valid hexadecimal representation or
   *         contains more than 8 bytes.
   */
  public static UInt32 fromHexString(String str) {
    return fromBytes(Bytes.fromHexStringLenient(str));
  }

  private static UInt32 create(int value) {
    if (value >= 0 && value <= MAX_CONSTANT) {
      return CONSTANTS[value];
    }
    return new UInt32(value);
  }

  private UInt32(int value) {
    this.value = value;
  }

  @Override
  public boolean isZero() {
    return this.value == 0;
  }

  @Override
  public UInt32 add(UInt32 value) {
    if (value.value == 0) {
      return this;
    }
    if (this.value == 0) {
      return value;
    }
    return create(this.value + value.value);
  }

  @Override
  public UInt32 add(int value) {
    if (value == 0) {
      return this;
    }
    return create(this.value + value);
  }

  @Override
  public UInt32 addMod(UInt32 value, UInt32 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    return create(toBigInteger().add(value.toBigInteger()).mod(modulus.toBigInteger()).intValue());
  }

  @Override
  public UInt32 addMod(long value, UInt32 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    return create(toBigInteger().add(BigInteger.valueOf(value)).mod(modulus.toBigInteger()).intValue());
  }

  @Override
  public UInt32 addMod(long value, long modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("addMod with zero modulus");
    }
    if (modulus < 0) {
      throw new ArithmeticException("addMod unsigned with negative modulus");
    }
    return create(toBigInteger().add(BigInteger.valueOf(value)).mod(BigInteger.valueOf(modulus)).intValue());
  }

  @Override
  public UInt32 subtract(UInt32 value) {
    if (value.isZero()) {
      return this;
    }
    return create(this.value - value.value);
  }

  @Override
  public UInt32 subtract(int value) {
    return add(-value);
  }

  @Override
  public UInt32 multiply(UInt32 value) {
    if (this.value == 0 || value.value == 0) {
      return ZERO;
    }
    if (value.value == 1) {
      return this;
    }
    return create(this.value * value.value);
  }

  @Override
  public UInt32 multiply(int value) {
    if (value < 0) {
      throw new ArithmeticException("multiply unsigned by negative");
    }
    if (value == 0 || this.value == 0) {
      return ZERO;
    }
    if (value == 1) {
      return this;
    }
    return create(this.value * value);
  }

  @Override
  public UInt32 multiplyMod(UInt32 value, UInt32 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (this.value == 0 || value.value == 0) {
      return ZERO;
    }
    if (value.value == 1) {
      return mod(modulus);
    }
    return create(toBigInteger().multiply(value.toBigInteger()).mod(modulus.toBigInteger()).intValue());
  }

  @Override
  public UInt32 multiplyMod(int value, UInt32 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (value == 0 || this.value == 0) {
      return ZERO;
    }
    if (value == 1) {
      return mod(modulus);
    }
    if (value < 0) {
      throw new ArithmeticException("multiplyMod unsigned by negative");
    }
    return create(toBigInteger().multiply(BigInteger.valueOf(value)).mod(modulus.toBigInteger()).intValue());
  }

  @Override
  public UInt32 multiplyMod(int value, int modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("multiplyMod with zero modulus");
    }
    if (modulus < 0) {
      throw new ArithmeticException("multiplyMod unsigned with negative modulus");
    }
    if (value == 0 || this.value == 0) {
      return ZERO;
    }
    if (value == 1) {
      return mod(modulus);
    }
    if (value < 0) {
      throw new ArithmeticException("multiplyMod unsigned by negative");
    }
    return create(toBigInteger().multiply(BigInteger.valueOf(value)).mod(BigInteger.valueOf(modulus)).intValue());
  }

  @Override
  public UInt32 divide(UInt32 value) {
    if (value.value == 0) {
      throw new ArithmeticException("divide by zero");
    }
    if (value.value == 1) {
      return this;
    }
    return create(toBigInteger().divide(value.toBigInteger()).intValue());
  }

  @Override
  public UInt32 divide(int value) {
    if (value == 0) {
      throw new ArithmeticException("divide by zero");
    }
    if (value < 0) {
      throw new ArithmeticException("divide unsigned by negative");
    }
    if (value == 1) {
      return this;
    }
    if (isPowerOf2(value)) {
      return shiftRight(log2(value));
    }
    return create(toBigInteger().divide(BigInteger.valueOf(value)).intValue());
  }

  @Override
  public UInt32 pow(UInt32 exponent) {
    return create(toBigInteger().modPow(exponent.toBigInteger(), P_2_32).intValue());
  }

  @Override
  public UInt32 pow(long exponent) {
    return create(toBigInteger().modPow(BigInteger.valueOf(exponent), P_2_32).intValue());
  }

  @Override
  public UInt32 mod(UInt32 modulus) {
    if (modulus.isZero()) {
      throw new ArithmeticException("mod by zero");
    }
    return create(toBigInteger().mod(modulus.toBigInteger()).intValue());
  }

  @Override
  public UInt32 mod(int modulus) {
    if (modulus == 0) {
      throw new ArithmeticException("mod by zero");
    }
    if (modulus < 0) {
      throw new ArithmeticException("mod by negative");
    }
    return create(this.value % modulus);
  }

  /**
   * Return a bit-wise AND of this value and the supplied value.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise AND
   */
  public UInt32 and(UInt32 value) {
    if (this.value == 0 || value.value == 0) {
      return ZERO;
    }
    return create(this.value & value.value);
  }

  /**
   * Return a bit-wise AND of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise AND
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt32 and(Bytes bytes) {
    checkArgument(bytes.size() <= 4, "and with more than 4 bytes");
    if (this.value == 0) {
      return ZERO;
    }
    int value = bytes.toInt();
    if (value == 0) {
      return ZERO;
    }
    return create(this.value & value);
  }

  /**
   * Return a bit-wise OR of this value and the supplied value.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise OR
   */
  public UInt32 or(UInt32 value) {
    return create(this.value | value.value);
  }

  /**
   * Return a bit-wise OR of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise OR
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt32 or(Bytes bytes) {
    checkArgument(bytes.size() <= 4, "or with more than 4 bytes");
    return create(this.value | bytes.toInt());
  }

  /**
   * Return a bit-wise XOR of this value and the supplied value.
   *
   * If this value and the supplied value are different lengths, then the shorter will be zero-padded to the left.
   *
   * @param value the value to perform the operation with
   * @return the result of a bit-wise XOR
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt32 xor(UInt32 value) {
    return create(this.value ^ value.value);
  }

  /**
   * Return a bit-wise XOR of this value and the supplied bytes.
   *
   * @param bytes the bytes to perform the operation with
   * @return the result of a bit-wise XOR
   * @throws IllegalArgumentException if more than 8 bytes are supplied
   */
  public UInt32 xor(Bytes bytes) {
    checkArgument(bytes.size() <= 4, "xor with more than 4 bytes");
    return create(this.value ^ bytes.toInt());
  }

  /**
   * Return a bit-wise NOT of this value.
   *
   * @return the result of a bit-wise NOT
   */
  public UInt32 not() {
    return create(~this.value);
  }

  /**
   * Shift all bits in this value to the right.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  public UInt32 shiftRight(int distance) {
    if (distance == 0) {
      return this;
    }
    if (distance >= 32) {
      return ZERO;
    }
    return create(this.value >>> distance);
  }

  /**
   * Shift all bits in this value to the left.
   *
   * @param distance The number of bits to shift by.
   * @return A value containing the shifted bits.
   */
  public UInt32 shiftLeft(int distance) {
    if (distance == 0) {
      return this;
    }
    if (distance >= 32) {
      return ZERO;
    }
    return create(this.value << distance);
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof UInt32)) {
      return false;
    }
    UInt32 other = (UInt32) object;
    return this.value == other.value;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.value);
  }

  @Override
  public int compareTo(UInt32 other) {
    return Long.compareUnsigned(this.value, other.value);
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @Override
  public BigInteger toBigInteger() {
    byte[] mag = new byte[4];
    mag[0] = (byte) ((this.value >>> 24) & 0xFF);
    mag[1] = (byte) ((this.value >>> 16) & 0xFF);
    mag[2] = (byte) ((this.value >>> 8) & 0xFF);
    mag[3] = (byte) (this.value & 0xFF);
    return new BigInteger(1, mag);
  }

  @Override
  public UInt32 toUInt32() {
    return this;
  }

  @Override
  public Bytes toBytes() {
    MutableBytes bytes = MutableBytes.create(4);
    bytes.setInt(0, this.value);
    return bytes;
  }

  @Override
  public Bytes toMinimalBytes() {
    int requiredBytes = 4 - (Integer.numberOfLeadingZeros(this.value) / 8);
    MutableBytes bytes = MutableBytes.create(requiredBytes);
    int j = 0;
    switch (requiredBytes) {
      case 4:
        bytes.set(j++, (byte) ((this.value >>> 24) & 0xFF));
        // fall through
      case 3:
        bytes.set(j++, (byte) ((this.value >>> 16) & 0xFF));
        // fall through
      case 2:
        bytes.set(j++, (byte) ((this.value >>> 8) & 0xFF));
        // fall through
      case 1:
        bytes.set(j, (byte) (this.value & 0xFF));
    }
    return bytes;
  }

  @Override
  public int numberOfLeadingZeros() {
    return Integer.numberOfLeadingZeros(this.value);
  }

  @Override
  public int bitLength() {
    return 32 - Integer.numberOfLeadingZeros(this.value);
  }

  private static boolean isPowerOf2(long n) {
    assert n > 0;
    return (n & (n - 1)) == 0;
  }

  private static int log2(int v) {
    assert v > 0;
    return 63 - Long.numberOfLeadingZeros(v);
  }
}
