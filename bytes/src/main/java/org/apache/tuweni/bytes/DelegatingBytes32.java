// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

/**
 * A class that holds and delegates all operations to its inner bytes field.
 *
 * <p>This class may be used to create more types that represent 32 bytes, but need a different name
 * for business logic.
 */
public class DelegatingBytes32 extends DelegatingBytes implements Bytes32 {

  protected DelegatingBytes32(Bytes delegate) {
    super(delegate);
  }

  @Override
  public int size() {
    return Bytes32.SIZE;
  }

  @Override
  public Bytes32 copy() {
    return Bytes32.wrap(toArray());
  }

  @Override
  public MutableBytes32 mutableCopy() {
    return MutableBytes32.wrap(toArray());
  }
}
