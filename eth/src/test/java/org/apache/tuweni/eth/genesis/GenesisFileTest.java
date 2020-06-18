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

import org.apache.tuweni.eth.Address;
import org.apache.tuweni.eth.Hash;
import org.apache.tuweni.junit.BouncyCastleExtension;
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

}
