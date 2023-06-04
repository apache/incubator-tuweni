// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.mikuli;

import org.apache.milagro.amcl.BLS381.BIG;

import java.util.Objects;

/** This class represents an ordinary scalar value. */
final class Scalar {

  private final BIG value;

  Scalar(BIG value) {
    this.value = value;
  }

  BIG value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Scalar scalar = (Scalar) o;
    return Objects.equals(value.toString(), scalar.value.toString());
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
