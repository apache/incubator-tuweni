// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.units.bigints;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;

import java.math.BigInteger;

public interface BytesUInt256Value<T extends UInt256Value<T>> extends Bytes32, UInt256Value<T> {

  @Override
  default long toLong() {
    return ((Bytes32) this).toLong();
  }

  @Override
  default String toHexString() {
    return ((Bytes32) this).toHexString();
  }

  @Override
  default String toShortHexString() {
    return ((Bytes) this).toShortHexString();
  }

  @Override
  default BigInteger toBigInteger() {
    return ((UInt256Value<T>) this).toBigInteger();
  }

  @Override
  default int numberOfLeadingZeros() {
    return ((Bytes) this).numberOfLeadingZeros();
  }

  @Override
  default boolean isZero() {
    return ((Bytes) this).isZero();
  }

  @Override
  default int bitLength() {
    return ((Bytes) this).bitLength();
  }
}
