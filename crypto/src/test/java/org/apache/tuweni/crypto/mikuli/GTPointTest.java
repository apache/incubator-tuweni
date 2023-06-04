// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.mikuli;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.milagro.amcl.BLS381.FP12;
import org.apache.tuweni.bytes.Bytes;

import org.junit.jupiter.api.Test;

class GTPointTest {

  @Test
  void equalsAndHashcode() {
    FP12 fp12 = FP12.fromBytes(Bytes.random(576).toArrayUnsafe());
    GTPoint point = new GTPoint(fp12);
    assertEquals(point, point);
    assertEquals(point.hashCode(), point.hashCode());
    assertEquals(new GTPoint(fp12), point);
    assertEquals(new GTPoint(fp12).hashCode(), point.hashCode());
  }
}
