// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.bytes;

import io.netty.buffer.Unpooled;

class ByteBufBytesTest extends CommonBytesTests {

  @Override
  Bytes h(String hex) {
    return Bytes.wrapByteBuf(Unpooled.copiedBuffer(Bytes.fromHexString(hex).toArrayUnsafe()));
  }

  @Override
  MutableBytes m(int size) {
    return MutableBytes.wrapByteBuf(Unpooled.copiedBuffer(new byte[size]));
  }

  @Override
  Bytes w(byte[] bytes) {
    return Bytes.wrapByteBuf(Unpooled.copiedBuffer(Bytes.of(bytes).toArray()));
  }

  @Override
  Bytes of(int... bytes) {
    return Bytes.wrapByteBuf(Unpooled.copiedBuffer(Bytes.of(bytes).toArray()));
  }
}
