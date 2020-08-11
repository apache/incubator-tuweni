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
