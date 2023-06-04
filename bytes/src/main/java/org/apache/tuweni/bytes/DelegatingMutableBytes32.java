// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.apache.tuweni.bytes.Checks.checkArgument;

final class DelegatingMutableBytes32 extends DelegatingMutableBytes implements MutableBytes32 {

  private DelegatingMutableBytes32(MutableBytes delegate) {
    super(delegate);
  }

  static MutableBytes32 delegateTo(MutableBytes value) {
    checkArgument(value.size() == SIZE, "Expected %s bytes but got %s", SIZE, value.size());
    return new DelegatingMutableBytes32(value);
  }

  @Override
  public Bytes32 copy() {
    return Bytes32.wrap(delegate.toArray());
  }

  @Override
  public MutableBytes32 mutableCopy() {
    return MutableBytes32.wrap(delegate.toArray());
  }
}
