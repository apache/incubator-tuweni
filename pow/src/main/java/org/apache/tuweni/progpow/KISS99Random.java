// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.progpow;

import org.apache.tuweni.units.bigints.UInt32;

final class KISS99Random {

  private static final UInt32 FILTER = UInt32.valueOf(65535);

  UInt32 z;
  UInt32 w;
  UInt32 jsr;
  UInt32 jcong;

  KISS99Random(UInt32 z, UInt32 w, UInt32 jsr, UInt32 jcong) {
    this.z = z;
    this.w = w;
    this.jsr = jsr;
    this.jcong = jcong;
  }

  UInt32 generate() {
    z = (z.and(FILTER).multiply(UInt32.valueOf(36969))).add(z.shiftRight(16));
    w = (w.and(FILTER).multiply(UInt32.valueOf(18000))).add(w.shiftRight(16));
    UInt32 mwc = z.shiftLeft(16).add(w);
    jsr = jsr.xor(jsr.shiftLeft(17));
    jsr = jsr.xor(jsr.shiftRight(13));
    jsr = jsr.xor(jsr.shiftLeft(5));
    jcong = (jcong.multiply(UInt32.valueOf(69069))).add(UInt32.valueOf(1234567));
    return mwc.xor(jcong).add(jsr);
  }
}
