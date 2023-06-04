// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

class DelegateBytesTest extends CommonBytesTests {

  @Override
  Bytes h(String hex) {
    return new DelegatingBytes(Bytes.fromHexString(hex));
  }

  @Override
  MutableBytes m(int size) {
    // no-op
    return MutableBytes.create(size);
  }

  @Override
  Bytes w(byte[] bytes) {
    return new DelegatingBytes(Bytes.wrap(bytes));
  }

  @Override
  Bytes of(int... bytes) {
    return new DelegatingBytes(Bytes.of(bytes));
  }
}
