// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.mikuli;

import org.apache.milagro.amcl.BLS381.FP12;
import org.apache.milagro.amcl.BLS381.PAIR;

/**
 * Function that maps 2 points on an elliptic curve to a number.
 */
final class AtePairing {

  /**
   *
   * Pair of points on the curve
   * 
   * @param p1 the point in Group1, not null
   * @param p2 the point in Group2, not null
   * @return GTPoint
   */
  static GTPoint pair(G1Point p1, G2Point p2) {
    FP12 e = PAIR.ate(p2.ecp2Point(), p1.ecpPoint());
    return new GTPoint(PAIR.fexp(e));
  }
}
