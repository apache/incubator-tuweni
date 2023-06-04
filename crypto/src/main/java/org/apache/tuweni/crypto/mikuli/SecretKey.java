// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.crypto.mikuli;

import static org.apache.milagro.amcl.BLS381.BIG.MODBYTES;

import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.tuweni.bytes.Bytes;

import java.util.Objects;

/** This class represents a BLS12-381 private key. */
public final class SecretKey {

  /**
   * Create a private key from a byte array
   *
   * @param bytes the bytes of the private key
   * @return a new SecretKey object
   */
  public static SecretKey fromBytes(byte[] bytes) {
    return fromBytes(Bytes.wrap(bytes));
  }

  /**
   * Create a private key from bytes
   *
   * @param bytes the bytes of the private key
   * @return a new SecretKey object
   */
  public static SecretKey fromBytes(Bytes bytes) {
    return new SecretKey(new Scalar(BIG.fromBytes(bytes.toArrayUnsafe())));
  }

  private final Scalar scalarValue;

  SecretKey(Scalar value) {
    this.scalarValue = value;
  }

  G2Point sign(G2Point message) {
    return message.mul(scalarValue);
  }

  public Bytes toBytes() {
    byte[] bytea = new byte[MODBYTES];
    scalarValue.value().toBytes(bytea);
    return Bytes.wrap(bytea);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SecretKey secretKey = (SecretKey) o;
    return Objects.equals(scalarValue, secretKey.scalarValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scalarValue);
  }
}
