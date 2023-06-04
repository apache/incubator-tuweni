// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import static org.apache.tuweni.bytes.Checks.checkArgument;

final class GuardedByteArrayBytes32 extends GuardedByteArrayBytes implements Bytes32 {

  GuardedByteArrayBytes32(byte[] bytes) {
    this(checkLength(bytes), 0);
  }

  GuardedByteArrayBytes32(byte[] bytes, int offset) {
    super(checkLength(bytes, offset), offset, SIZE);
  }

  // Ensures a proper error message.
  private static byte[] checkLength(byte[] bytes) {
    checkArgument(bytes.length == SIZE, "Expected %s bytes but got %s", SIZE, bytes.length);
    return bytes;
  }

  // Ensures a proper error message.
  private static byte[] checkLength(byte[] bytes, int offset) {
    checkArgument(
        bytes.length - offset >= SIZE,
        "Expected at least %s bytes from offset %s but got only %s",
        SIZE,
        offset,
        bytes.length - offset);
    return bytes;
  }

  @Override
  public Bytes32 copy() {
    return new ArrayWrappingBytes32(toArray());
  }

  @Override
  public MutableBytes32 mutableCopy() {
    return new MutableArrayWrappingBytes32(toArray());
  }
}
