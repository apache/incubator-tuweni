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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.units.bigints.UInt32;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Testing ethash hashing.
 */
@ExtendWith(BouncyCastleExtension.class)
class EthashTest {

  @Test
  void testBlock300005() {
    long blockNumber = 300005L;
    long nonce = 2170677771517793035L;
    Bytes mixHash = Bytes.fromHexString("0xd1e82d611846e4b162ad3ba0f129611c3a67f2c3aeda19ad862765cf64b383f6");
    Bytes contentToHash = Bytes.fromHexString("0x783b5c2bc6f879509cd69009cb28fecf004d63833d7e444109b7ab9e327ac866");
    long datasetSize = EthHash.getFullSize(blockNumber);
    long cacheSize = EthHash.getCacheSize(blockNumber);
    assertEquals(1157627776, datasetSize);
    assertEquals(18087488, cacheSize);
    UInt32[] cache = EthHash.mkCache((int) cacheSize, blockNumber);
    Bytes hash = EthHash.hashimotoLight(datasetSize, cache, contentToHash, Bytes.ofUnsignedLong(nonce));
    assertEquals(mixHash, hash.slice(0, 32));
  }


}
