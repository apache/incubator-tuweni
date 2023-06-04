// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.mikuli;

import org.apache.tuweni.bytes.Bytes;

import java.util.List;
import java.util.Objects;

/** This class represents a Signature on G2 */
public final class Signature {

  /**
   * Aggregates list of Signature pairs
   *
   * @param signatures The list of signatures to aggregate, not null
   * @throws IllegalArgumentException if parameter list is empty
   * @return Signature, not null
   */
  public static Signature aggregate(List<Signature> signatures) {
    if (signatures.isEmpty()) {
      throw new IllegalArgumentException("Parameter list is empty");
    }
    return signatures.stream().reduce(Signature::combine).get();
  }

  /**
   * Decode a signature from its serialized representation.
   *
   * @param bytes the bytes of the signature
   * @return the signature
   */
  public static Signature decode(Bytes bytes) {
    G2Point point = G2Point.fromBytes(bytes);
    return new Signature(point);
  }

  private final G2Point point;

  Signature(G2Point point) {
    this.point = point;
  }

  /**
   * Combines this signature with another signature, creating a new signature.
   *
   * @param signature the signature to combine with
   * @return a new signature as combination of both signatures.
   */
  public Signature combine(Signature signature) {
    return new Signature(point.add(signature.point));
  }

  /**
   * Signature serialization
   *
   * @return byte array representation of the signature, not null
   */
  public Bytes encode() {
    return point.toBytes();
  }

  @Override
  public String toString() {
    return "Signature [ecpPoint=" + point.toString() + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((point == null) ? 0 : point.hashCode());
    return result;
  }

  G2Point g2Point() {
    return point;
  }

  @Override
  public boolean equals(Object obj) {
    if (Objects.isNull(obj)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Signature)) {
      return false;
    }
    Signature other = (Signature) obj;
    return point.equals(other.point);
  }
}
