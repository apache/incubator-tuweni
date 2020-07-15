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
    return UInt32
        .fromBytes(UInt64.fromBytes(x.toBytes()).multiply(UInt64.fromBytes(y.toBytes())).toBytes().slice(0, 4));
  }

  private static UInt32 clz(UInt32 value) {
    return UInt32.valueOf(value.numberOfLeadingZeros());
  }

  private static UInt32 popcount(UInt32 value) {
    return UInt32.valueOf(Long.bitCount(value.toLong()));
  }

  static UInt32 rotl32(UInt32 var, UInt32 hops) {
    return var
        .shiftLeft(hops.mod(UInt32.valueOf(32)).intValue())
        .or(var.shiftRight(UInt32.valueOf(32).subtract(hops.mod(UInt32.valueOf(32))).intValue()));
  }

  static UInt32 rotr32(UInt32 var, UInt32 hops) {
    return var
        .shiftRight(hops.mod(UInt32.valueOf(32)).intValue())
        .or(var.shiftLeft(UInt32.valueOf(32).subtract(hops.mod(UInt32.valueOf(32))).intValue()));
  }
}
