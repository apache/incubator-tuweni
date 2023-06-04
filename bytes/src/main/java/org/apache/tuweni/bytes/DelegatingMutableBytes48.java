// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.apache.tuweni.bytes.Checks.checkArgument;

final class DelegatingMutableBytes48 extends DelegatingMutableBytes implements MutableBytes48 {

  private DelegatingMutableBytes48(MutableBytes delegate) {
    super(delegate);
  }

  static MutableBytes48 delegateTo(MutableBytes value) {
    checkArgument(value.size() == SIZE, "Expected %s bytes but got %s", SIZE, value.size());
    return new DelegatingMutableBytes48(value);
  }

  @Override
  public Bytes48 copy() {
    return Bytes48.wrap(delegate.toArray());
  }

  @Override
  public MutableBytes48 mutableCopy() {
    return MutableBytes48.wrap(delegate.toArray());
  }
}
