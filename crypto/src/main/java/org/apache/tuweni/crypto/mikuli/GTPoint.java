// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.mikuli;

import org.apache.milagro.amcl.BLS381.FP12;

import java.util.Objects;

/**
 * GT is the object that holds the result of the pairing operation. Points in GT are elements of Fq12.
 */
final class GTPoint {

  private final FP12 point;

  GTPoint(FP12 point) {
    this.point = point;
  }

  @Override
  public int hashCode() {
    return Objects.hash(point);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;
    GTPoint gtPoint = (GTPoint) o;
    return (point != null && gtPoint.point == null) || point.equals(gtPoint.point);
  }
}
