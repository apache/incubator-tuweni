/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.kv;

import net.consensys.cava.bytes.Bytes;

import java.nio.ByteBuffer;
import javax.annotation.Nullable;

import io.lettuce.core.codec.RedisCodec;

class RedisBytesCodec implements RedisCodec<Bytes, Bytes> {

  @Override
  public ByteBuffer encodeKey(@Nullable Bytes key) {
    if (key == null) {
      return ByteBuffer.allocate(0);
    }
    return ByteBuffer.wrap(key.toArrayUnsafe());
  }

  @Override
  public Bytes decodeKey(@Nullable ByteBuffer bytes) {
    if (bytes == null) {
      return null;
    }
    return Bytes.wrapByteBuffer(bytes);
  }

  @Override
  public ByteBuffer encodeValue(@Nullable Bytes value) {
    if (value == null) {
      return ByteBuffer.allocate(0);
    }
    return ByteBuffer.wrap(value.toArrayUnsafe());
  }

  @Override
  public Bytes decodeValue(@Nullable ByteBuffer bytes) {
    if (bytes == null) {
      return null;
    }
    return Bytes.wrapByteBuffer(bytes);
  }
}
