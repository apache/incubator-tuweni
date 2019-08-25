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
package org.apache.tuweni.progpow;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.ethash.EthHash;
import org.apache.tuweni.units.bigints.UInt32;
import org.apache.tuweni.units.bigints.UInt64;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Ethereum ProgPoW mining algorithm, based on revision 0.9.2.
 *
 * This implements the ProgPoW algorithm (https://github.com/ifdefelse/ProgPOW). This algorithm is licensed under CC0
 * 1.0 Universal (CC0 1.0) Public Domain Dedication (https://creativecommons.org/publicdomain/zero/1.0/)
 *
 * @implSpec https://github.com/ifdefelse/ProgPOW
 */
public final class ProgPoW {

  static int PROGPOW_PERIOD = 50;
  static int PROGPOW_LANES = 16;
  static int PROGPOW_REGS = 32;
  static int PROGPOW_DAG_LOADS = 4;
  static int PROGPOW_CACHE_BYTES = 16 * 1024;
  static int PROGPOW_CNT_DAG = 64;
  static int PROGPOW_CNT_CACHE = 12;
  static int PROGPOW_CNT_MATH = 20;
  static UInt32 FNV_PRIME = UInt32.fromHexString("0x1000193");
  static Bytes FNV_OFFSET_BASIS = Bytes.fromHexString("0x811c9dc5");
  static int HASH_BYTES = 64;
  static int HASH_WORDS = 16;
  static int DATASET_PARENTS = 256;

  /**
   * Creates a hash using the ProgPoW formulation of a block
   *
   * @param blockNumber the block number of the block
   * @param nonce the nonce of the block
   * @param header the header of the block
   * @param dag the directed acyclic graph cache
   * @param dagLookupFunction the function to append to the DAG
   * @return a hash matching the block input, using the ProgPoW algorithm
   */
  public static Bytes32 progPowHash(
      long blockNumber,
      long nonce,
      Bytes32 header,
      UInt32[] dag, // gigabyte DAG located in framebuffer - the first portion gets cached
      Function<Integer, Bytes> dagLookupFunction) {
    UInt32[][] mix = new UInt32[PROGPOW_LANES][PROGPOW_REGS];

    // keccak(header..nonce)
    Bytes32 seed_256 = Keccakf800.keccakF800Progpow(header, nonce, Bytes32.ZERO);

    // endian swap so byte 0 of the hash is the MSB of the value
    long seed = (Integer.toUnsignedLong(seed_256.getInt(0)) << 32) | Integer.toUnsignedLong(seed_256.getInt(4));
    // initialize mix for all lanes
    for (int l = 0; l < PROGPOW_LANES; l++)
      mix[l] = fillMix(UInt64.fromBytes(Bytes.wrap(ByteBuffer.allocate(8).putLong(seed).array())), UInt32.valueOf(l));
    // execute the randomly generated inner loop
    for (int i = 0; i < PROGPOW_CNT_DAG; i++)
      progPowLoop(blockNumber, UInt32.valueOf(i), mix, dag, dagLookupFunction);

    // Reduce mix data to a per-lane 32-bit digest
    UInt32[] digest_lane = new UInt32[PROGPOW_LANES];
    for (int l = 0; l < PROGPOW_LANES; l++) {
      digest_lane[l] = UInt32.fromBytes(FNV_OFFSET_BASIS);
      for (int i = 0; i < PROGPOW_REGS; i++)
        digest_lane[l] = fnv1a(digest_lane[l], mix[l][i]);
    }
    // Reduce all lanes to a single 256-bit digest
    UInt32[] digest = new UInt32[8];
    Arrays.fill(digest, UInt32.fromBytes(FNV_OFFSET_BASIS));

    for (int l = 0; l < PROGPOW_LANES; l++) {
      digest[l % 8] = fnv1a(digest[l % 8], digest_lane[l]);
    }

    Bytes32 bytesDigest = Bytes32.wrap(Bytes.concatenate(Stream.of(digest).map(UInt32::toBytes).toArray(Bytes[]::new)));

    // keccak(header .. keccak(header..nonce) .. digest);
    return Keccakf800.keccakF800Progpow(header, seed, bytesDigest);
  }

  /**
   * Creates a cache for the DAG at a given block number
   * 
   * @param blockNumber the block number
   * @param datasetLookup the function generating elements of the DAG
   * @return a cache of the DAG up to the block number
   */
  public static UInt32[] createDagCache(long blockNumber, Function<Integer, Bytes> datasetLookup) {
    // TODO size of cache should be function of blockNumber - and DAG should be stored in its own memory structure.
    // cache the first 16KB of the dag
    UInt32[] cdag = new UInt32[HASH_BYTES * DATASET_PARENTS];
    for (int i = 0; i < cdag.length; i++) {
      // this could be sped up 16x
      Bytes lookup = datasetLookup.apply(i >> 4);
      cdag[i] = UInt32.fromBytes(lookup.slice((i & 0xf) << 2, 4).reverse());
    }
    return cdag;
  }

  static KISS99Random progPowInit(UInt64 prog_seed, int[] mix_seq_src, int[] mix_seq_dst) {
    UInt32 leftSeed = UInt32.fromBytes(prog_seed.toBytes().slice(0, 4));
    UInt32 rightSeed = UInt32.fromBytes(prog_seed.toBytes().slice(4));
    UInt32 z = fnv1a(UInt32.fromBytes(FNV_OFFSET_BASIS), rightSeed);
    UInt32 w = fnv1a(z, leftSeed);
    UInt32 jsr = fnv1a(w, rightSeed);
    UInt32 jcong = fnv1a(jsr, leftSeed);
    KISS99Random prog_rnd = new KISS99Random(z, w, jsr, jcong);

    for (int i = 0; i < PROGPOW_REGS; i++) {
      mix_seq_dst[i] = i;
      mix_seq_src[i] = i;
    }
    for (int i = PROGPOW_REGS - 1; i > 0; i--) {
      int j = prog_rnd.generate().mod(UInt32.valueOf(i + 1)).intValue();
      int buffer = mix_seq_dst[i];
      mix_seq_dst[i] = mix_seq_dst[j];
      mix_seq_dst[j] = buffer;
      j = prog_rnd.generate().mod(UInt32.valueOf(i + 1)).intValue();
      buffer = mix_seq_src[i];
      mix_seq_src[i] = mix_seq_src[j];
      mix_seq_src[j] = buffer;
    }
    return prog_rnd;
  }

  static UInt32 merge(UInt32 a, UInt32 b, UInt32 r) {
    switch (r.mod(UInt32.valueOf(4)).intValue()) {
      case 0:
        return (a.multiply(UInt32.valueOf(33))).add(b);
      case 1:
        return a.xor(b).multiply(UInt32.valueOf(33));
      // prevent rotate by 0 which is a NOP
      case 2:
        return ProgPoWMath.rotl32(a, (r.shiftRight(16).mod(UInt32.valueOf(31))).add(UInt32.ONE)).xor(b);
      case 3:
        return ProgPoWMath.rotr32(a, (r.shiftRight(16).mod(UInt32.valueOf(31))).add(UInt32.ONE)).xor(b);
      default:
        throw new IllegalArgumentException(
            "r mod 4 is larger than 4" + r.toHexString() + " " + r.mod(UInt32.valueOf(4)).intValue());
    }
  }

  static UInt32[] fillMix(UInt64 seed, UInt32 laneId) {

    UInt32 z = fnv1a(UInt32.fromBytes(FNV_OFFSET_BASIS), UInt32.fromBytes(seed.toBytes().slice(4, 4)));
    UInt32 w = fnv1a(z, UInt32.fromBytes(seed.toBytes().slice(0, 4)));
    UInt32 jsr = fnv1a(w, laneId);
    UInt32 jcong = fnv1a(jsr, laneId);

    KISS99Random random = new KISS99Random(z, w, jsr, jcong);

    UInt32[] mix = new UInt32[ProgPoW.PROGPOW_REGS];
    for (int i = 0; i < mix.length; i++) {
      mix[i] = random.generate();
    }

    return mix;
  }

  static UInt32 fnv1a(UInt32 h, UInt32 d) {
    return (h.xor(d)).multiply(FNV_PRIME);
  }

  static void progPowLoop(
      long blockNumber,
      UInt32 loop,
      UInt32[][] mix,
      UInt32[] dag,
      Function<Integer, Bytes> dagLookupFunction) {

    long dagBytes = EthHash.getFullSize(blockNumber);

    // dag_entry holds the 256 bytes of data loaded from the DAG
    UInt32[][] dag_entry = new UInt32[PROGPOW_LANES][PROGPOW_DAG_LOADS];
    // On each loop iteration rotate which lane is the source of the DAG address.
    // The source lane's mix[0] value is used to ensure the last loop's DAG data feeds into this loop's address.
    // dag_addr_base is which 256-byte entry within the DAG will be accessed
    int dag_addr_base = mix[loop.intValue() % PROGPOW_LANES][0]
        .mod(UInt32.valueOf(java.lang.Math.toIntExact(dagBytes / (PROGPOW_LANES * PROGPOW_DAG_LOADS * Integer.BYTES))))
        .intValue();
    //mix[loop.intValue()%PROGPOW_LANES][0].mod(UInt32.valueOf((int) dagBytes / (PROGPOW_LANES*PROGPOW_DAG_LOADS*4))).intValue();
    for (int l = 0; l < PROGPOW_LANES; l++) {
      // Lanes access DAG_LOADS sequential words from the dag entry
      // Shuffle which portion of the entry each lane accesses each iteration by XORing lane and loop.
      // This prevents multi-chip ASICs from each storing just a portion of the DAG
      //
      int dag_addr_lane = dag_addr_base * PROGPOW_LANES + Integer.remainderUnsigned(l ^ loop.intValue(), PROGPOW_LANES);
      int offset = Integer.remainderUnsigned(l ^ loop.intValue(), PROGPOW_LANES);
      for (int i = 0; i < PROGPOW_DAG_LOADS; i++) {
        UInt32 lookup = (UInt32.valueOf(dag_addr_lane).divide(UInt32.valueOf(4))).add(offset >> 4);
        Bytes lookupHolder = dagLookupFunction.apply(lookup.intValue());
        int lookupOffset = (i * 4 + ((offset & 0xf) << 4)) % 64;
        dag_entry[l][i] = UInt32.fromBytes(lookupHolder.slice(lookupOffset, 4).reverse());

      }

    }

    // Initialize the program seed and sequences
    // When mining these are evaluated on the CPU and compiled away
    int[] mix_seq_dst = new int[PROGPOW_REGS];
    int[] mix_seq_src = new int[PROGPOW_REGS];
    int mix_seq_dst_cnt = 0;
    int mix_seq_src_cnt = 0;
    KISS99Random prog_rnd = progPowInit(UInt64.valueOf(blockNumber / PROGPOW_PERIOD), mix_seq_src, mix_seq_dst);

    int max_i = Integer.max(PROGPOW_CNT_CACHE, PROGPOW_CNT_MATH);
    for (int i = 0; i < max_i; i++) {
      if (i < PROGPOW_CNT_CACHE) {
        // Cached memory access
        // lanes access random 32-bit locations within the first portion of the DAG
        int src = mix_seq_src[(mix_seq_src_cnt++) % PROGPOW_REGS];
        int dst = mix_seq_dst[(mix_seq_dst_cnt++) % PROGPOW_REGS];
        UInt32 sel = prog_rnd.generate();
        for (int l = 0; l < PROGPOW_LANES; l++) {
          UInt32 offset = mix[l][src].mod(UInt32.valueOf(PROGPOW_CACHE_BYTES / 4));
          mix[l][dst] = merge(mix[l][dst], dag[offset.intValue()], sel);
        }
      }

      if (i < PROGPOW_CNT_MATH) {
        // Random ProgPoWMath
        // Generate 2 unique sources
        UInt32 src_rnd = prog_rnd.generate().mod(UInt32.valueOf(PROGPOW_REGS * (PROGPOW_REGS - 1)));
        int src1 = src_rnd.mod(UInt32.valueOf(PROGPOW_REGS)).intValue(); // 0 <= src1 < PROGPOW_REGS
        int src2 = src_rnd.divide(UInt32.valueOf(PROGPOW_REGS)).intValue(); // 0 <= src2 < PROGPOW_REGS - 1
        if (src2 >= src1)
          ++src2; // src2 is now any reg other than src1
        UInt32 sel1 = prog_rnd.generate();
        int dst = mix_seq_dst[(mix_seq_dst_cnt++) % PROGPOW_REGS];
        UInt32 sel2 = prog_rnd.generate();
        for (int l = 0; l < PROGPOW_LANES; l++) {
          UInt32 data = ProgPoWMath.math(mix[l][src1], mix[l][src2], sel1);
          mix[l][dst] = merge(mix[l][dst], data, sel2);
        }
      }
    }

    // Consume the global load data at the very end of the loop to allow full latency hiding
    // Always merge into mix[0] to feed the offset calculation
    for (int i = 0; i < PROGPOW_DAG_LOADS; i++) {
      int dst = (i == 0) ? 0 : mix_seq_dst[(mix_seq_dst_cnt++) % PROGPOW_REGS];
      UInt32 sel = prog_rnd.generate();
      for (int l = 0; l < PROGPOW_LANES; l++) {
        mix[l][dst] = merge(mix[l][dst], dag_entry[l][i], sel);
      }
    }
  }
}
