// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.progpow;

import static org.apache.tuweni.units.bigints.UInt32s.min;

import org.apache.tuweni.units.bigints.UInt32;
import org.apache.tuweni.units.bigints.UInt64;

final class ProgPoWMath {

  static UInt32 math(UInt32 a, UInt32 b, UInt32 r) {
    switch (r.mod(UInt32.valueOf(11)).intValue()) {
      case 0:
        return a.add(b);
      case 1:
        return a.multiply(b);
      case 2:
        return mul_hi(a, b);
      case 3:
        return min(a, b);
      case 4:
        return rotl32(a, b);
      case 5:
        return rotr32(a, b);
      case 6:
        return a.and(b);
      case 7:
        return a.or(b);
      case 8:
        return a.xor(b);
      case 9:
        return clz(a).add(clz(b));
      case 10:
        return popcount(a).add(popcount(b));
      default:
        throw new IllegalArgumentException(
            "Value " + r + " has mod larger than 11 " + r.mod(UInt32.valueOf(11).intValue()));
    }
  }

  private static UInt32 mul_hi(UInt32 x, UInt32 y) {
    return UInt32.fromBytes(
        UInt64.fromBytes(x.toBytes())
            .multiply(UInt64.fromBytes(y.toBytes()))
            .toBytes()
            .slice(0, 4));
  }

  private static UInt32 clz(UInt32 value) {
    return UInt32.valueOf(value.numberOfLeadingZeros());
  }

  private static UInt32 popcount(UInt32 value) {
    return UInt32.valueOf(Long.bitCount(value.toLong()));
  }

  static UInt32 rotl32(UInt32 var, UInt32 hops) {
    return var.shiftLeft(hops.mod(UInt32.valueOf(32)).intValue())
        .or(var.shiftRight(UInt32.valueOf(32).subtract(hops.mod(UInt32.valueOf(32))).intValue()));
  }

  static UInt32 rotr32(UInt32 var, UInt32 hops) {
    return var.shiftRight(hops.mod(UInt32.valueOf(32)).intValue())
        .or(var.shiftLeft(UInt32.valueOf(32).subtract(hops.mod(UInt32.valueOf(32))).intValue()));
  }
}
