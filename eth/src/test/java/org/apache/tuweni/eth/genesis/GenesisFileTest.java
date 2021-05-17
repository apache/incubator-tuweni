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
package org.apache.tuweni.eth.genesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.apache.tuweni.bytes.Bytes;
import org.apache.tuweni.eth.Address;
import org.apache.tuweni.eth.Hash;
import org.apache.tuweni.junit.BouncyCastleExtension;
import org.apache.tuweni.units.bigints.UInt256;
import org.apache.tuweni.units.bigints.UInt64;
import org.apache.tuweni.units.ethereum.Gas;
import org.apache.tuweni.units.ethereum.Wei;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class GenesisFileTest {

  @Test
  void testReadGenesisFile() throws IOException {
    InputStream input = GenesisFileTest.class.getResourceAsStream("/valid-genesis.json");
    byte[] contents = input.readAllBytes();
    GenesisFile file = GenesisFile.read(contents);
    assertNotNull(file.toBlock());
    assertEquals(0, file.toBlock().getBody().getTransactions().size());
  }

  @Test
  void testMainnetGenesisFile() throws IOException {
    InputStream input = GenesisFileTest.class.getResourceAsStream("/mainnet.json");
    byte[] contents = input.readAllBytes();
    GenesisFile file = GenesisFile.read(contents);
    assertEquals(8893, file.getAllocations().size());
    assertEquals(
        Wei.valueOf(new BigInteger("327600000000000000000")),
        file.getAllocations().get(Address.fromHexString("c951900c341abbb3bafbf7ee2029377071dbc36a")));
    assertEquals(
        Hash.fromHexString("0xd7f8974fb5ac78d9ac099b9ad5018bedc2ce0a72dad1827a1709da30580f0544"),
        file.toBlock().getHeader().getStateRoot());
    Hash expectedHash = Hash.fromHexString("0xd4e56740f876aef8c010b86a40d5f56745a118d0906a34e69aec8c0db1cb8fa3");
    assertEquals(8, file.getForks().size());
    assertEquals((Long) 1150000L, file.getForks().get(0));
    assertEquals(expectedHash, file.toBlock().getHeader().getHash());
  }

  @Test
  void testMissingNonce() throws IOException {
    InputStream input = GenesisFileTest.class.getResourceAsStream("/missing-nonce.json");
    byte[] contents = input.readAllBytes();
    assertThrows(IllegalArgumentException.class, () -> GenesisFile.read(contents));
  }

  @Test
  void testMissingDifficulty() throws IOException {
    InputStream input = GenesisFileTest.class.getResourceAsStream("/missing-difficulty.json");
    byte[] contents = input.readAllBytes();
    assertThrows(IllegalArgumentException.class, () -> GenesisFile.read(contents));
  }

  @Test
  void testBesuDev() throws IOException {
    InputStream input = GenesisFileTest.class.getResourceAsStream("/besu-dev.json");
    byte[] contents = input.readAllBytes();
    GenesisFile file = GenesisFile.read(contents);
    assertEquals(3, file.getAllocations().size());
    assertEquals(UInt64.valueOf(66L), file.toBlock().getHeader().getNonce());
    assertEquals(
        Address.fromHexString("0x0000000000000000000000000000000000000000"),
        file.toBlock().getHeader().getCoinbase());
    assertEquals(
        Hash.fromHexString("0x0000000000000000000000000000000000000000000000000000000000000000"),
        file.toBlock().getHeader().getMixHash());
    assertEquals(Gas.valueOf(UInt256.fromHexString("0x1fffffffffffff")), file.toBlock().getHeader().getGasLimit());
    assertEquals(UInt256.fromHexString("0x10000"), file.toBlock().getHeader().getDifficulty());
    assertEquals(
        Bytes.fromHexString("0x11bbe8db4e347b4e8c937c1c8370e4b5ed33adb3db69cbdb7a38e1e50b1b82fa"),
        file.toBlock().getHeader().getExtraData());
    assertEquals(
        file.getAllocations().get(Address.fromHexString("fe3b557e8fb62b89f4916b721be55ceb828dbd73")),
        Wei.valueOf(UInt256.fromHexString("0xad78ebc5ac6200000")));
    assertEquals(
        file.getAllocations().get(Address.fromHexString("627306090abaB3A6e1400e9345bC60c78a8BEf57")),
        Wei.valueOf(new BigInteger("90000000000000000000000")));
    assertEquals(
        file.getAllocations().get(Address.fromHexString("f17f52151EbEF6C7334FAD080c5704D77216b732")),
        Wei.valueOf(new BigInteger("90000000000000000000000")));
    assertEquals(
        Hash.fromHexString("0x166ed98eea93ab2b6f6b1a425526994adc2d675bf9a0d77d600ed1e02d8f77df"),
        file.toBlock().getHeader().getStateRoot());
    assertEquals(
        Hash.fromHexString("0xa08d1edb37ba1c62db764ef7c2566cbe368b850f5b3762c6c24114a3fd97b87f"),
        file.toBlock().getHeader().getHash());
  }

  @Test
  void testAstorGenesis() throws IOException {
    InputStream input = GenesisFileTest.class.getResourceAsStream("/astor.json");
    byte[] contents = input.readAllBytes();
    GenesisFile file = GenesisFile.read(contents);
    assertEquals(
        Hash.fromHexString("0xf9c534ad514bc380e23a74e788c3d1f2446f9ad1cb74f0dbb48ea56c0315f5bc"),
        file.toBlock().getHeader().getHash());
  }

}
