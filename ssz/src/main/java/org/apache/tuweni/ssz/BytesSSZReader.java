/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.tuweni.ssz;

import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.bigints.UInt384;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;

final class BytesSSZReader implements SSZReader {

  private final Bytes content;
  private int index = 0;

  BytesSSZReader(Bytes content) {
    this.content = content;
  }

  @Override
  public Bytes readBytes(int limit) {
    int byteLength = 4;
    ensureBytes(byteLength, () -> "SSZ encoded data is not a byte array");
    int size;
    try {
      size = content.getInt(index, LITTLE_ENDIAN);
    } catch (IndexOutOfBoundsException e) {
      throw new EndOfSSZException();
    }
    if (size < 0 || size > limit) {
      throw new InvalidSSZTypeException("length of bytes would exceed limit");
    }
    index += 4;
    if (content.size() - index - size < 0) {
      throw new InvalidSSZTypeException("SSZ encoded data has insufficient bytes for decoded byte array length");
    }
    return consumeBytes(size);
  }

  @Override
  public Bytes readFixedBytes(int byteLength, int limit) {
    ensureBytes(byteLength, () -> "SSZ encoded data is not a fixed-length byte array");
    return consumeBytes(byteLength);
  }

  @Override
  public int readInt(int bitLength) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    int byteLength = bitLength / 8;
    ensureBytes(byteLength, () -> "SSZ encoded data has insufficient length to read a " + bitLength + "-bit integer");
    Bytes bytes = content.slice(index, byteLength);
    int zeroBytes = bytes.numberOfTrailingZeroBytes();
    if ((byteLength - zeroBytes) > 4) {
      throw new InvalidSSZTypeException("decoded integer is too large for an int");
    }
    index += byteLength;
    return bytes.slice(0, bytes.size() - zeroBytes).toInt(LITTLE_ENDIAN);
  }

  @Override
  public long readLong(int bitLength) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    int byteLength = bitLength / 8;
    ensureBytes(byteLength, () -> "SSZ encoded data has insufficient length to read a " + bitLength + "-bit integer");
    Bytes bytes = content.slice(index, byteLength);
    int zeroBytes = bytes.numberOfTrailingZeroBytes();
    if ((byteLength - zeroBytes) > 8) {
      throw new InvalidSSZTypeException("decoded integer is too large for a long");
    }
    index += byteLength;
    return bytes.slice(0, bytes.size() - zeroBytes).toLong(LITTLE_ENDIAN);
  }

  @Override
  public BigInteger readBigInteger(int bitLength) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    int byteLength = bitLength / 8;
    ensureBytes(byteLength, () -> "SSZ encoded data has insufficient length to read a " + bitLength + "-bit integer");
    return consumeBytes(byteLength).toBigInteger(LITTLE_ENDIAN);
  }

  @Override
  public BigInteger readUnsignedBigInteger(int bitLength) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    int byteLength = bitLength / 8;
    ensureBytes(byteLength, () -> "SSZ encoded data has insufficient length to read a " + bitLength + "-bit integer");
    return consumeBytes(byteLength).toUnsignedBigInteger(LITTLE_ENDIAN);
  }

  @Override
  public UInt256 readUInt256() {
    ensureBytes(256 / 8, () -> "SSZ encoded data has insufficient length to read a 256-bit integer");
    return UInt256.fromBytes(consumeBytes(256 / 8).reverse());
  }

  @Override
  public UInt384 readUInt384() {
    ensureBytes(384 / 8, () -> "SSZ encoded data has insufficient length to read a 384-bit integer");
    return UInt384.fromBytes(consumeBytes(384 / 8).reverse());
  }

  @Override
  public Bytes readAddress() {
    ensureBytes(20, () -> "SSZ encoded data has insufficient length to read a 20-byte address");
    return consumeBytes(20);
  }

  @Override
  public Bytes readHash(int hashLength) {
    ensureBytes(hashLength, () -> "SSZ encoded data has insufficient length to read a " + hashLength + "-byte hash");
    return consumeBytes(hashLength);
  }

  @Override
  public List<Bytes> readBytesList(int limit) {
    return readList(remaining -> readBytes(limit));
  }

  @Override
  public List<Bytes> readVector(long listSize, int limit) {
    return readList(listSize, remaining -> readByteArray(limit), Bytes::wrap);
  }

  @Override
  public List<Bytes> readFixedBytesVector(int listSize, int byteLength, int limit) {
    return readFixedList(listSize, remaining -> readFixedByteArray(byteLength, limit), Bytes::wrap);
  }

  @Override
  public List<Bytes> readFixedBytesList(int byteLength, int limit) {
    return readList(byteLength, () -> readFixedBytes(byteLength, limit));
  }

  @Override
  public List<String> readStringList(int limit) {
    return readList(remaining -> readBytes(limit), bytes -> new String(bytes.toArrayUnsafe(), UTF_8));
  }

  @Override
  public List<Integer> readIntList(int bitLength) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    return readList(bitLength / 8, () -> readInt(bitLength));
  }

  @Override
  public List<Long> readLongIntList(int bitLength) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    return readList(bitLength / 8, () -> readLong(bitLength));
  }

  @Override
  public List<BigInteger> readBigIntegerList(int bitLength) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    return readList(bitLength / 8, () -> readBigInteger(bitLength));
  }

  @Override
  public List<BigInteger> readUnsignedBigIntegerList(int bitLength) {
    if (bitLength % 8 != 0) {
      throw new IllegalArgumentException("bitLength must be a multiple of 8");
    }
    return readList(bitLength / 8, () -> readUnsignedBigInteger(bitLength));
  }

  @Override
  public List<UInt256> readUInt256List() {
    return readList(256 / 8, this::readUInt256);
  }

  @Override
  public List<UInt384> readUInt384List() {
    return readList(384 / 8, this::readUInt384);
  }

  @Override
  public List<Bytes> readAddressList() {
    return readList(20, this::readAddress);
  }

  @Override
  public List<Bytes> readHashList(int hashLength) {
    return readList(hashLength, () -> readHash(hashLength));
  }

  @Override
  public List<Boolean> readBooleanList() {
    return readList(1, this::readBoolean);
  }

  @Override
  public boolean isComplete() {
    return index >= content.size();
  }

  private void ensureBytes(int byteLength, Supplier<String> message) {
    if (index == content.size()) {
      throw new EndOfSSZException();
    }
    if (content.size() - index - byteLength < 0) {
      throw new InvalidSSZTypeException(message.get());
    }
  }

  private Bytes consumeBytes(int size) {
    Bytes bytes = content.slice(index, size);
    index += size;
    return bytes;
  }

  private List<Bytes> readList(LongFunction<Bytes> bytesSupplier) {
    ensureBytes(4, () -> "SSZ encoded data is not a list");
    int originalIndex = this.index;
    List<Bytes> elements;
    try {
      // use a long to simulate reading unsigned
      long listSize = consumeBytes(4).toLong(LITTLE_ENDIAN);
      elements = new ArrayList<>();
      while (listSize > 0) {
        Bytes bytes = bytesSupplier.apply(listSize);
        elements.add(bytes);
        listSize -= bytes.size();
        listSize -= 4;
        if (listSize < 0) {
          throw new InvalidSSZTypeException("SSZ encoded list length does not align with lengths of its elements");
        }
      }
    } catch (Exception e) {
      this.index = originalIndex;
      throw e;
    }
    return elements;
  }

  private <T> List<T> readList(LongFunction<Bytes> bytesSupplier, Function<Bytes, T> converter) {
    ensureBytes(4, () -> "SSZ encoded data is not a list");
    int originalIndex = this.index;
    List<T> elements;
    try {
      // use a long to simulate reading unsigned
      long listSize = consumeBytes(4).toLong(LITTLE_ENDIAN);
      elements = new ArrayList<>();
      while (listSize > 0) {
        Bytes bytes = bytesSupplier.apply(listSize);
        elements.add(converter.apply(bytes));
        listSize -= bytes.size();
        listSize -= 4;
        if (listSize < 0) {
          throw new InvalidSSZTypeException("SSZ encoded list length does not align with lengths of its elements");
        }
      }
    } catch (Exception e) {
      this.index = originalIndex;
      throw e;
    }
    return elements;
  }

  private <T> List<T> readList(long listSize, LongFunction<byte[]> bytesSupplier, Function<byte[], T> converter) {
    int originalIndex = this.index;
    List<T> elements;
    try {
      elements = new ArrayList<>();
      while (listSize > 0) {
        byte[] bytes = bytesSupplier.apply(listSize);
        elements.add(converter.apply(bytes));
        // When lists have lengths passed in, the listSize argument is the number of
        // elements in the list, instead of the number of bytes in the list, so
        // we only subtract one each time an element is processed in this case.
        listSize -= 1;
        if (listSize < 0) {
          throw new InvalidSSZTypeException("SSZ encoded list length does not align with lengths of its elements");
        }
      }
    } catch (Exception e) {
      this.index = originalIndex;
      throw e;
    }
    return elements;
  }

  private <T> List<T> readFixedList(int listSize, LongFunction<byte[]> bytesSupplier, Function<byte[], T> converter) {
    int originalIndex = this.index;
    List<T> elements;
    try {
      elements = new ArrayList<>();
      while (listSize > 0) {
        byte[] bytes = bytesSupplier.apply(listSize);
        elements.add(converter.apply(bytes));
        // When lists have lengths passed in, the listSize argument is the number of
        // elements in the list, instead of the number of bytes in the list, so
        // we only subtract one each time an element is processed in this case.
        listSize -= 1;
        if (listSize < 0) {
          throw new InvalidSSZTypeException("SSZ encoded list length does not align with lengths of its elements");
        }
      }
    } catch (Exception e) {
      this.index = originalIndex;
      throw e;
    }
    return elements;
  }

  private <T> List<T> readList(int elementSize, Supplier<T> elementSupplier) {
    ensureBytes(4, () -> "SSZ encoded data is not a list");
    int originalIndex = this.index;
    List<T> bytesList;
    try {
      int listSize = consumeBytes(4).toInt(LITTLE_ENDIAN);
      if ((listSize % elementSize) != 0) {
        throw new InvalidSSZTypeException("SSZ encoded list length does not align with lengths of its elements");
      }
      int nElements = listSize / elementSize;
      bytesList = new ArrayList<>(nElements);
      for (int i = 0; i < nElements; ++i) {
        bytesList.add(elementSupplier.get());
      }
    } catch (Exception e) {
      this.index = originalIndex;
      throw e;
    }
    return bytesList;
  }
}
