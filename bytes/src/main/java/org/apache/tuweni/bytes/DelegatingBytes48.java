// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

/**
 * A class that holds and delegates all operations to its inner bytes field.
 *
 * <p>This class may be used to create more types that represent 48 bytes, but need a different name
 * for business logic.
 */
public class DelegatingBytes48 extends DelegatingBytes implements Bytes48 {

  protected DelegatingBytes48(Bytes delegate) {
    super(delegate);
  }

  @Override
  public int size() {
    return Bytes48.SIZE;
  }

  @Override
  public Bytes48 copy() {
    return Bytes48.wrap(toArray());
  }

  @Override
  public MutableBytes48 mutableCopy() {
    return MutableBytes48.wrap(toArray());
  }
}
