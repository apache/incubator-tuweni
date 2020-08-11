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
package org.apache.tuweni.ethash;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.Hash;
import org.apache.tuweni.units.bigints.UInt32;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Implementation of EthHash utilities for Ethereum mining algorithms.
 */
public class EthHash {

  /**
   * Bytes in word.
   */
  public static int WORD_BYTES = 4;
  /**
   * bytes in dataset at genesis
   */
  public static long DATASET_BYTES_INIT = (long) Math.pow(2, 30);
  /**
   * dataset growth per epoch
   */
  public static long DATASET_BYTES_GROWTH = (long) Math.pow(2, 23);
  /**
   * bytes in cache at genesis
   */
  public static long CACHE_BYTES_INIT = (long) Math.pow(2, 24);

  /**
   * cache growth per epoch
   */
  public static long CACHE_BYTES_GROWTH = (long) Math.pow(2, 17);
  /**
   * Size of the DAG relative to the cache
   */
  public static int CACHE_MULTIPLIER = 1024;
  /**
   * blocks per epoch
   */
  public static int EPOCH_LENGTH = 30000;
  /**
   * width of mix
   */
  public static int MIX_BYTES = 128;

  /**
   * hash length in bytes
   */
  public static int HASH_BYTES = 64;
  /**
   * Number of words in a hash
   */
  private static int HASH_WORDS = HASH_BYTES / WORD_BYTES;
  /**
   * number of parents of each dataset element
   */
  public static int DATASET_PARENTS = 256;
  /**
   * number of rounds in cache production
   */
  public static int CACHE_ROUNDS = 3;
  /**
   * number of accesses in hashimoto loop
   */
  public static int ACCESSES = 64;

  public static int FNV_PRIME = 0x01000193;

  /**
   * Hashimoto Light Hashing.
   *
   * @param size the size of the full data set
   * @param cache EthHash Cache
   * @param header Truncated BlockHeader hash
   * @param nonce Nonce to use for hashing
   * @return A byte array holding MixHash in its first 32 bytes and the EthHash result in the bytes 32 to 63
   */
  public static Bytes hashimotoLight(long size, UInt32[] cache, Bytes header, Bytes nonce) {
    return hashimoto(size, header, nonce, (ind) -> calcDatasetItem(cache, ind));
  }

  private static Bytes hashimoto(long size, Bytes header, Bytes nonce, Function<UInt32, Bytes> datasetLookup) {
    long n = Long.divideUnsigned(size, MIX_BYTES);
    Bytes seed = Hash.keccak512(Bytes.concatenate(header, nonce.reverse()));
    Bytes[] mixBufferElts = new Bytes[MIX_BYTES / HASH_BYTES];
    for (int i = 0; i < MIX_BYTES / HASH_BYTES; ++i) {
      mixBufferElts[i] = seed;
    }
    Bytes mixBuffer = Bytes.concatenate(mixBufferElts);
    UInt32[] mix = new UInt32[MIX_BYTES / 4];
    for (int i = 0; i < MIX_BYTES / 4; ++i) {
      mix[i] = UInt32.fromBytes(mixBuffer.slice(i * 4, 4), ByteOrder.LITTLE_ENDIAN);
    }

    UInt32 firstSeed = mix[0];
    for (int i = 0; i < ACCESSES; ++i) {
      UInt32 fnvValue = fnv(firstSeed.xor(i), mix[i % (MIX_BYTES / WORD_BYTES)]);
      UInt32 p = fnvValue.mod((int) n);
      List<UInt32> cache = new ArrayList<>();
      for (int j = 0; j < MIX_BYTES / HASH_BYTES; ++j) {
        Bytes lookupResult = datasetLookup.apply((p.multiply(2)).add(j));
        for (int k = 0; k < lookupResult.size() / 4; k++) {
          cache.add(UInt32.fromBytes(lookupResult.slice(k * 4, 4), ByteOrder.LITTLE_ENDIAN));
        }
      }
      for (int k = 0; k < mix.length; k++) {
        mix[k] = fnv(mix[k], cache.get(k));
      }
    }
    Stream<Bytes> part1 = IntStream.range(0, mix.length / 4).parallel().mapToObj((int i) -> {
      int index = i * 4;
      return fnv(fnv(fnv(mix[index], mix[index + 1]), mix[index + 2]), mix[index + 3]).toBytes().reverse();
    });
    Bytes resultPart1 = Bytes.concatenate(part1.toArray(Bytes[]::new));
    Bytes resultPart2 = Hash.keccak256(Bytes.concatenate(seed, resultPart1));

    return Bytes.concatenate(resultPart1, resultPart2);
  }

  /**
   * Calculates the EthHash Epoch for a given block number.
   *
   * @param block Block Number
   * @return EthHash Epoch
   */
  public static long epoch(long block) {
    return block / EPOCH_LENGTH;
  }

  /**
   * Provides the size of the cache at a given block number
   *
   * @param block_number the block number
   * @return the size of the cache at the block number, in bytes
   */
  public static int getCacheSize(long block_number) {
    long sz = CACHE_BYTES_INIT + CACHE_BYTES_GROWTH * (block_number / EPOCH_LENGTH);
    sz -= HASH_BYTES;
    while (!isPrime(sz / HASH_BYTES)) {
      sz -= 2 * HASH_BYTES;
    }
    return (int) sz;
  }

  /**
   * Provides the size of the full dataset at a given block number
   *
   * @param block_number the block number
   * @return the size of the full dataset at the block number, in bytes
   */
  public static long getFullSize(long block_number) {
    long sz = DATASET_BYTES_INIT + DATASET_BYTES_GROWTH * (block_number / EPOCH_LENGTH);
    sz -= MIX_BYTES;
    while (!isPrime(sz / MIX_BYTES)) {
      sz -= 2 * MIX_BYTES;
    }
    return sz;
  }

  /**
   * Generates the EthHash cache for given parameters.
   *
   * @param cacheSize Size of the cache to generate
   * @param block Block Number to generate cache for
   * @return EthHash Cache
   */
  public static UInt32[] mkCache(int cacheSize, long block) {
    int rows = cacheSize / HASH_BYTES;
    List<Bytes> cache = new ArrayList<>(rows);
    cache.add(Hash.keccak512(dagSeed(block)));

    for (int i = 1; i < rows; ++i) {
      cache.add(Hash.keccak512(cache.get(i - 1)));
    }

    Bytes completeCache = Bytes.concatenate(cache.toArray(new Bytes[cache.size()]));

    byte[] temp = new byte[HASH_BYTES];
    for (int i = 0; i < CACHE_ROUNDS; ++i) {
      for (int j = 0; j < rows; ++j) {
        int offset = j * HASH_BYTES;
        for (int k = 0; k < HASH_BYTES; ++k) {
          temp[k] = (byte) (completeCache.get((j - 1 + rows) % rows * HASH_BYTES + k)
              ^ (completeCache
                  .get(
                      Integer.remainderUnsigned(completeCache.getInt(offset, ByteOrder.LITTLE_ENDIAN), rows)
                          * HASH_BYTES
                          + k)));
        }
        temp = Hash.keccak512(temp);
        System.arraycopy(temp, 0, completeCache.toArrayUnsafe(), offset, HASH_BYTES);
      }
    }
    UInt32[] result = new UInt32[completeCache.size() / 4];
    for (int i = 0; i < result.length; i++) {
      result[i] = UInt32.fromBytes(completeCache.slice(i * 4, 4).reverse());
    }

    return result;
  }

  /**
   * Calculate a data set item based on the previous cache for a given index
   *
   * @param cache the DAG cache
   * @param index the current index
   * @return a new DAG item to append to the DAG
   */
  public static Bytes calcDatasetItem(UInt32[] cache, UInt32 index) {
    int rows = cache.length / HASH_WORDS;
    UInt32[] mixInts = new UInt32[HASH_BYTES / 4];
    int offset = index.mod(rows).multiply(HASH_WORDS).intValue();
    mixInts[0] = cache[offset].xor(index);
    System.arraycopy(cache, offset + 1, mixInts, 1, HASH_WORDS - 1);
    Bytes buffer = intToByte(mixInts);
    buffer = Hash.keccak512(buffer);
    for (int i = 0; i < mixInts.length; i++) {
      mixInts[i] = UInt32.fromBytes(buffer.slice(i * 4, 4).reverse());
    }
    for (int i = 0; i < DATASET_PARENTS; ++i) {
      fnvHash(
          mixInts,
          cache,
          fnv(index.xor(UInt32.valueOf(i)), mixInts[i % 16])
              .mod(UInt32.valueOf(rows))
              .multiply(UInt32.valueOf(HASH_WORDS)));
    }
    return Hash.keccak512(intToByte(mixInts));
  }

  private static Bytes dagSeed(long block) {
    Bytes32 seed = Bytes32.wrap(new byte[32]);
    if (Long.compareUnsigned(block, EPOCH_LENGTH) >= 0) {
      for (int i = 0; i < Long.divideUnsigned(block, EPOCH_LENGTH); i++) {
        seed = Hash.keccak256(seed);
      }
    }
    return seed;
  }

  private static UInt32 fnv(UInt32 v1, UInt32 v2) {
    return (v1.multiply(FNV_PRIME)).xor(v2);
  }

  private static void fnvHash(UInt32[] mix, UInt32[] cache, UInt32 offset) {
    for (int i = 0; i < mix.length; i++) {
      mix[i] = fnv(mix[i], cache[offset.intValue() + i]);
    }
  }

  private static Bytes intToByte(UInt32[] ints) {
    return Bytes.concatenate(Stream.of(ints).map(i -> i.toBytes().reverse()).toArray(Bytes[]::new));
  }

  private static boolean isPrime(long number) {
    return number > 2 && IntStream.rangeClosed(2, (int) Math.sqrt(number)).noneMatch(n -> (number % n == 0));
  }
}
